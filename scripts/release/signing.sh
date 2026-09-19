#!/usr/bin/env bash
set -euo pipefail
umask 077
export GIT_TERMINAL_PROMPT=0

check_access() {
  git ls-remote "$1" HEAD >/dev/null 2>&1 || return 1
  if [ "${RELEASE_SIGNING_WRITE:-false}" = true ]; then
    # Ask GitHub's receive service to authorize a push without sending updates.
    git push --dry-run "$1" "HEAD:refs/heads/signing-access-check-${GITHUB_RUN_ID:-$$}" \
      >/dev/null 2>&1 || return 1
  fi
}

try_ssh() {
  [ -n "${MATCH_GIT_SSH_KEY:-}" ] || return 1
  local key="$RUNNER_TEMP/release-match-key"
  printf '%s\n' "$MATCH_GIT_SSH_KEY" > "$key" || return 1
  ssh-keyscan -p 443 ssh.github.com > "$RUNNER_TEMP/release-known-hosts" 2>/dev/null || return 1
  local ssh_command="ssh -i '$key' -o IdentitiesOnly=yes -o Hostname=ssh.github.com -p 443 -o UserKnownHostsFile='$RUNNER_TEMP/release-known-hosts' -o StrictHostKeyChecking=yes -o BatchMode=yes -o ConnectTimeout=15"
  if ! GIT_SSH_COMMAND="$ssh_command" check_access git@github.com:depollsoft/certificates.git; then
    echo 'SSH credential could not access the signing repository with the required permissions.' >&2
    return 1
  fi
  {
    echo "GIT_SSH_COMMAND=$ssh_command"
    echo 'MATCH_GIT_URL=git@github.com:depollsoft/certificates.git'
    echo 'MATCH_GIT_BASIC_AUTHORIZATION='
  } >> "$GITHUB_ENV" || { echo 'Could not persist verified signing credentials.' >&2; exit 1; }
  echo 'Verified signing repository access using SSH.'
}

try_https() {
  [ -n "${MATCH_GIT_BASIC_AUTHORIZATION:-}" ] || return 1
  local authorization
  authorization="$(python3 - <<'PY'
import base64
import binascii
import os

def strip_prefix(value):
    value = value.strip()
    for prefix in ('authorization: basic ', 'basic '):
        if value.lower().startswith(prefix):
            return value[len(prefix):].strip()
    return value

raw = strip_prefix(os.environ['MATCH_GIT_BASIC_AUTHORIZATION'])
try:
    decoded = strip_prefix(base64.b64decode(raw, validate=True).decode('utf-8'))
except (binascii.Error, UnicodeDecodeError):
    decoded = ''
credential = decoded if ':' in decoded else raw if ':' in raw else 'x-access-token:' + raw
print(base64.b64encode(credential.encode()).decode())
PY
)" || return 1
  echo "::add-mask::$authorization"
  if ! GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=http.extraHeader \
    GIT_CONFIG_VALUE_0="Authorization: Basic $authorization" \
    check_access https://github.com/depollsoft/certificates.git; then
    echo 'HTTPS credential could not access the signing repository with the required permissions.' >&2
    return 1
  fi
  {
    echo 'GIT_SSH_COMMAND='
    echo 'MATCH_GIT_URL=https://github.com/depollsoft/certificates.git'
    echo "MATCH_GIT_BASIC_AUTHORIZATION=$authorization"
  } >> "$GITHUB_ENV" || { echo 'Could not persist verified signing credentials.' >&2; exit 1; }
  echo 'Verified signing repository access using HTTPS.'
}

# Provisioning needs write access; preview deploy keys are commonly read-only.
if [ "${RELEASE_SIGNING_WRITE:-false}" = true ]; then
  try_https || try_ssh || { echo 'No signing credential grants read/write access to depollsoft/certificates.' >&2; exit 1; }
else
  try_ssh || try_https || { echo 'No signing credential grants read access to depollsoft/certificates.' >&2; exit 1; }
fi
