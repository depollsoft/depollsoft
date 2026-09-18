"""Reject tracked user exports and signing credentials. Never print their contents."""

from fnmatch import fnmatchcase
from pathlib import PurePosixPath
import subprocess
import sys


def forbidden(path: str) -> bool:
    if path == 'Android/debug.jks':
        return False
    name = PurePosixPath(path).name.lower()
    return (
        name == 'fabric.properties'
        or path.startswith('CloudCode/') and path.endswith('/config/global.json')
        or path.startswith('Firebase/') and name in {'allusers', 'users.json', 'users.csv'}
        or name == '.env'
        or name.startswith('.env.') and name != '.env.example'
        or any(fnmatchcase(name, pattern) for pattern in (
            '*.jks', '*.keystore', '*.p12', '*.pfx', '*.p8', '*.mobileprovision',
            '*service-account*.json', '*service_account*.json',
        ))
    )


if __name__ == '__main__':
    paths = subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0')
    blocked = [path for path in paths if path and forbidden(path)]
    if blocked:
        print('Remove sensitive files from Git; use private storage or CI secrets:', file=sys.stderr)
        print('\n'.join(blocked), file=sys.stderr)
        sys.exit(1)
    print('No prohibited credential or user-export files are tracked.')
