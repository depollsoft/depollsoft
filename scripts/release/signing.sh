#!/usr/bin/env bash
set -euo pipefail
if [ -n "$MATCH_GIT_SSH_KEY" ]; then
  key="$RUNNER_TEMP/release-match-key"
  umask 077
  printf '%s\n' "$MATCH_GIT_SSH_KEY" > "$key"
  ssh-keyscan -p 443 ssh.github.com > "$RUNNER_TEMP/release-known-hosts"
  echo "GIT_SSH_COMMAND=ssh -i $key -o IdentitiesOnly=yes -o Hostname=ssh.github.com -p 443 -o UserKnownHostsFile=$RUNNER_TEMP/release-known-hosts -o StrictHostKeyChecking=yes" >> "$GITHUB_ENV"
  echo 'MATCH_GIT_URL=git@github.com:depollsoft/certificates.git' >> "$GITHUB_ENV"
elif [ -z "$MATCH_GIT_BASIC_AUTHORIZATION" ]; then
  echo 'Missing signing repository credentials' >&2
  exit 1
fi
