#!/usr/bin/env python3
"""Serialize simulator tests, stream their logs, and bound startup and test hangs."""
import codecs
import fcntl
import os
from pathlib import Path
import re
import selectors
import signal
import subprocess
import sys
import time

import ios_simulator

CASE_STARTED = re.compile(r"^Test [Cc]ase '(.+)' started")
CASE_FINISHED = re.compile(r"^Test [Cc]ase '(.+)' (passed|failed|skipped)(?: on| \()")


def signal_command(child, signum):
    # The command owns a separate process group. Never signal another job's tools.
    try:
        os.killpg(child.pid, signum)
    except ProcessLookupError:
        pass


def run(command, lock_path=Path('/tmp/depollsoft-ios-simulator.lock'), *,
        fail_fast=False, startup_timeout=300, test_timeout=30, shutdown_timeout=15):
    child = None
    # Only explicit iOS Simulator destinations belong to this invocation.
    simulator = next((match[1] for value in command
                      if (match := re.search(r'platform=iOS Simulator,id=([A-Fa-f0-9-]{36})', value))), None)

    def interrupted(_signum, _frame):
        raise KeyboardInterrupt

    signal.signal(signal.SIGINT, interrupted)
    signal.signal(signal.SIGTERM, interrupted)
    # /tmp is shared even when runner slots have different TMPDIR values.
    with lock_path.open('a') as lock:
        started = time.monotonic()
        print('Waiting for the host iOS simulator slot', flush=True)
        fcntl.flock(lock, fcntl.LOCK_EX)
        print(f'Acquired simulator slot after {time.monotonic() - started:.1f}s', flush=True)
        try:
            if simulator:
                ios_simulator.clean_abandoned_captures()
                ios_simulator.boot(simulator)
            child = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                     start_new_session=True,
                                     env=dict(os.environ, NSUnbufferedIO='YES'))
            started = last_output = last_notice = time.monotonic()
            first_test_seen = False
            active_case = None
            case_started = None
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
                # Release the shared slot only after our simulator stops using it.
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
