#!/usr/bin/env python3
"""Run a simulator command exclusively across runner slots on this Mac."""
import fcntl
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time


def run(command, lock_path=Path('/tmp/depollsoft-ios-simulator.lock'), *, fail_fast=False):
    child = None

    def interrupted(signum, _frame):
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
            child = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                     text=True, bufsize=1, env=dict(os.environ, NSUnbufferedIO='YES'))
            for line in child.stdout:
                print(line, end='', flush=True)
                if fail_fast and re.match(r"^Test [Cc]ase '.+' failed(?: on| \()", line):
                    print('::error::Stopping after the first failed test: ' + line.strip(), flush=True)
                    child.send_signal(signal.SIGINT)
                    try:
                        remaining, _ = child.communicate(timeout=15)
                        print(remaining, end='', flush=True)
                    except subprocess.TimeoutExpired:
                        child.kill()
                        remaining, _ = child.communicate()
                        print(remaining, end='', flush=True)
                    return 1
            return child.wait()
        finally:
            if child is not None and child.poll() is None:
                child.terminate()
                try:
                    child.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    child.kill()
                    child.wait()


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit('Provide a command to run')
    try:
        status = run(sys.argv[1:], fail_fast=True)
    except KeyboardInterrupt:
        status = 130
    raise SystemExit(status if status >= 0 else 128 - status)
