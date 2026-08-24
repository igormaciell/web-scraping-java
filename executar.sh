#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f dist/extrator-estatisticas-esportivas.jar ]]; then
    ./compilar.sh
fi

exec java -jar dist/extrator-estatisticas-esportivas.jar "$@"
