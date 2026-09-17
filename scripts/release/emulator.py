#!/usr/bin/env python3
"""Run a capture on an owned emulator with bounded startup and shutdown."""
import argparse
import os
from pathlib import Path
import platform
import signal
import socket
import subprocess
import tempfile
import time


def check_port_available(port):
    with socket.socket() as probe:
        # A closed emulator connection can leave this port in TIME_WAIT.
        # Allow those connections, but still reject an active listener.
        probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        probe.bind(('127.0.0.1', port))


def stop_process_group(process, grace=10):
    """Stop only the session we created, including children holding log handles."""
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except ProcessLookupError:
        pass
    try:
        process.wait(timeout=grace)
    except subprocess.TimeoutExpired:
        pass
    # The leader may have exited while a child is still alive.
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except ProcessLookupError:
        pass
    process.wait(timeout=5)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--name', required=True, choices=['watch-round', 'watch-square', 'phone-tablet'])
    parser.add_argument('--port', type=int, default=5554)
    parser.add_argument('--boot-timeout', type=int, default=300)
    parser.add_argument('command', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ['--'] else args.command
    if not command:
        parser.error('Provide a capture command after --')
    if args.port < 5554 or args.port > 5682 or args.port % 2:
        parser.error('Use an even emulator console port between 5554 and 5682')
    for port in (args.port, args.port + 1):
        check_port_available(port)
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    adb = [str(sdk / 'platform-tools/adb'), '-s', f'emulator-{args.port}']
    arch = 'arm64-v8a' if platform.machine() in ('arm64', 'aarch64') else 'x86_64'
    watch = args.name.startswith('watch-')
    api, target = (33, 'android-wear') if watch else (35, 'google_apis')
    device = {'watch-round': 'wearos_small_round', 'watch-square': 'wearos_square',
              'phone-tablet': 'pixel_7'}[args.name]
    package = f'system-images;android-{api};{target};{arch}'
    logs = Path('build/release/emulator-logs') / args.name
    logs.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='release-avd-') as folder:
        env = dict(os.environ, ANDROID_AVD_HOME=folder)
        with (logs / 'setup.log').open('w') as setup:
            subprocess.run(['sdkmanager', 'emulator', package], env=env, stdout=setup, stderr=subprocess.STDOUT,
                           check=True, timeout=600)
            subprocess.run(['avdmanager', 'create', 'avd', '--name', args.name, '--package', package,
                            '--device', device], input='no\n', text=True, env=env, stdout=setup,
                           stderr=subprocess.STDOUT, check=True, timeout=60)
        config = Path(folder) / f'{args.name}.avd/config.ini'
        # Profile defaults and image minimums vary. Write each key exactly once.
        values = dict(line.split('=', 1) for line in config.read_text().splitlines() if '=' in line)
        values.update({'hw.cpu.ncore': '2', 'hw.ramSize': '2048',
                       'disk.dataPartition.size': '2048M' if watch else '4096M'})
        if watch:
            values['hw.lcd.circular'] = 'true' if args.name == 'watch-round' else 'false'
        config.write_text(''.join(f'{key}={value}\n' for key, value in values.items()))
        options = ['-no-window', '-gpu', 'swiftshader_indirect', '-no-snapshot', '-noaudio',
                   '-no-boot-anim', '-camera-back', 'none', '-dns-server', '8.8.8.8']
        if not watch:
            options += ['-skin', '1600x2560']
        with (logs / 'emulator.log').open('w') as log:
            emulator = subprocess.Popen([str(sdk / 'emulator/emulator'), '-avd', args.name,
                                         '-port', str(args.port), *options], env=env,
                                        stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                deadline = time.monotonic() + args.boot_timeout
                while time.monotonic() < deadline:
                    if emulator.poll() is not None:
                        raise RuntimeError(f'Emulator exited; see {logs / "emulator.log"}')
                    try:
                        boot = subprocess.run([*adb, 'shell', 'getprop', 'sys.boot_completed'],
                                              capture_output=True, text=True, timeout=10)
                        if boot.stdout.strip() == '1':
                            break
                    except subprocess.TimeoutExpired:
                        pass
                    time.sleep(2)
                else:
                    raise TimeoutError(f'Emulator did not boot within {args.boot_timeout}s')
                print(f'{args.name} booted; starting capture', flush=True)
                subprocess.run([*adb, 'shell', 'input', 'keyevent', '82'], check=True, timeout=15)
                capture = subprocess.Popen(command, env=env, start_new_session=True)
                try:
                    if capture.wait(timeout=3600):
                        raise subprocess.CalledProcessError(capture.returncode, command)
                finally:
                    stop_process_group(capture)
            except BaseException:
                for filename, diagnostic in (
                    ('logcat.txt', ['logcat', '-d', '-t', '1500']),
                    ('screen.png', ['exec-out', 'screencap', '-p']),
                ):
                    try:
                        with (logs / filename).open('wb') as handle:
                            subprocess.run([*adb, *diagnostic], stdout=handle,
                                           stderr=subprocess.DEVNULL, timeout=15)
                    except (OSError, subprocess.TimeoutExpired):
                        pass
                raise
            finally:
                try:
                    subprocess.run([*adb, 'emu', 'kill'], capture_output=True, timeout=10)
                except subprocess.TimeoutExpired:
                    pass
                stop_process_group(emulator)
                print(f'{args.name} stopped', flush=True)


if __name__ == '__main__':
    def interrupted(_signum, _frame):
        raise KeyboardInterrupt('Capture interrupted')

    signal.signal(signal.SIGTERM, interrupted)
    main()
