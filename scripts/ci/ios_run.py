#!/usr/bin/env python3
"""Stream native test logs and bound startup and test hangs."""
import codecs
import os
import re
import selectors
import signal
import subprocess
import sys
import time
import threading
from pathlib import Path

import ios_simulator

CASE_STARTED = re.compile(r"^Test [Cc]ase '(.+)' started")
CASE_FINISHED = re.compile(r"^Test [Cc]ase '(.+)' (passed|failed|skipped)(?: on| \()")


def sample_simulator_apps(simulator):
    """Collect stacks from this device only, without delaying the watchdog."""
    try:
        processes = subprocess.check_output(['ps', '-axo', 'pid=,comm='], text=True, timeout=2)
        for line in processes.splitlines():
            fields = line.strip().split(None, 1)
            if len(fields) != 2:
                continue
            pid, executable = fields
            if f'/Devices/{simulator}/data/Containers/Bundle/Application/' not in executable:
                continue
            destination = Path(f'simulator-{simulator}-{pid}.sample.txt')
            subprocess.run(['sample', pid, '1', '10', '-file', str(destination)],
                           capture_output=True, timeout=4, check=True)
            print(f'Captured slow-test stack: {destination}', flush=True)
    except (subprocess.SubprocessError, OSError) as error:
        print(f'Slow-test stack capture unavailable: {error}', flush=True)


def signal_command(child, signum):
    # The command owns a separate process group. Never signal another job's tools.
    try:
        os.killpg(child.pid, signum)
    except ProcessLookupError:
        pass


def run(command, *,
        fail_fast=False, startup_timeout=300, test_timeout=30, shutdown_timeout=15):
    child = None
    # Only explicit iOS Simulator destinations belong to this invocation.
    simulator = next((match[1] for value in command
                      if (match := re.search(r'platform=iOS Simulator,id=([A-Fa-f0-9-]{36})', value))), None)

    def interrupted(_signum, _frame):
        raise KeyboardInterrupt

    signal.signal(signal.SIGINT, interrupted)
    signal.signal(signal.SIGTERM, interrupted)
    try:
        if simulator:
            ios_simulator.boot(simulator)
        child = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                 start_new_session=True,
                                 env=dict(os.environ, NSUnbufferedIO='YES'))
        started = last_output = last_notice = time.monotonic()
        first_test_seen = False
        active_case = None
        case_started = None
        sampled_case = False
        stopping = None
        buffered = ''
        decoder = codecs.getincrementaldecoder('utf-8')(errors='replace')

        def stop(reason):
            nonlocal stopping
            if stopping is None:
                print('::error::' + reason, flush=True)
                stopping = time.monotonic()
                signal_command(child, signal.SIGINT)

        # Reading a line directly can block forever when XCTest or an inherited
        # output pipe hangs. Poll both the pipe and the process instead.
        with selectors.DefaultSelector() as selector:
            selector.register(child.stdout, selectors.EVENT_READ)
            while True:
                now = time.monotonic()
                if selector.select(timeout=.1):
                    chunk = os.read(child.stdout.fileno(), 65536)
                    if not chunk:
                        break
                    last_output = now
                    decoded = decoder.decode(chunk)
                    print(decoded, end='', flush=True)
                    buffered += decoded
                    while '\n' in buffered:
                        line, buffered = buffered.split('\n', 1)
                        begin = CASE_STARTED.match(line)
                        end = CASE_FINISHED.match(line)
                        if begin:
                            first_test_seen = True
                            active_case = begin[1]
                            case_started = now
                            sampled_case = False
                        if end:
                            first_test_seen = True
                            active_case = None
                            if fail_fast and end[2] == 'failed':
                                stop('Stopping after the first failed test: ' + line)
                elif child.poll() is not None:
                    break
                if stopping is not None:
                    if now - stopping >= shutdown_timeout:
                        signal_command(child, signal.SIGKILL)
                        break
                elif fail_fast:
                    if simulator and active_case and not sampled_case and now - case_started >= 20:
                        sampled_case = True
                        threading.Thread(target=sample_simulator_apps, args=(simulator,), daemon=True).start()
                    if not first_test_seen and now - started >= startup_timeout:
                        stop(f'XCTest did not start a test within {startup_timeout:g}s. See the raw log above.')
                    elif active_case and now - case_started >= test_timeout:
                        stop(f'{active_case} exceeded the {test_timeout:g}s test budget.')
                    elif now - last_output >= startup_timeout:
                        stop(f'XCTest produced no output for {startup_timeout:g}s.')
                    elif not first_test_seen and now - last_notice >= 30:
                        print(f'Waiting for XCTest to start its first test ({now - started:.0f}s elapsed)', flush=True)
                        last_notice = now
        return 1 if stopping is not None else child.wait(timeout=shutdown_timeout)
    finally:
        if child is not None:
            signal_command(child, signal.SIGTERM)
            try:
                child.wait(timeout=shutdown_timeout)
            except subprocess.TimeoutExpired:
                signal_command(child, signal.SIGKILL)
                child.wait(timeout=5)
            child.stdout.close()
        if simulator:
            # Stop only this command's simulator; other jobs keep running.
            ios_simulator.shutdown(simulator)


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit('Provide a command to run')
    try:
        # Scheduled UI jobs build before testing; regular CI reuses its build.
        startup = 900 if 'test' in sys.argv[1:] else 300
        status = run(sys.argv[1:], fail_fast=True, startup_timeout=startup)
    except KeyboardInterrupt:
        status = 130
    raise SystemExit(status if status >= 0 else 128 - status)
