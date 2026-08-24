#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
./compilar.sh

mkdir -p build/test-classes
find src/test/java -name '*.java' -print | sort > build/fontes-testes.txt

javac --release 17 -encoding UTF-8 \
    -cp build/classes \
    -d build/test-classes \
    @build/fontes-testes.txt

java -ea -cp "build/classes:build/test-classes" br.edu.linguagens.esportes.TodosOsTestes
