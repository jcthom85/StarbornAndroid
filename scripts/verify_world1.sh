#!/bin/sh
set -eu
exec python3 "$(dirname "$0")/maestro_runner.py" verify "$@"
