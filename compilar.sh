#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes dist

find src/main/java -name '*.java' -print | sort > build/fontes-principais.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/fontes-principais.txt
cp -R src/main/resources/. build/classes/

cat > build/MANIFEST.MF <<'MANIFEST'
Manifest-Version: 1.0
Main-Class: br.edu.linguagens.esportes.Aplicacao
Implementation-Title: Extrator de Estatisticas Esportivas
Implementation-Version: 1.0.0

MANIFEST

jar --create \
    --file dist/extrator-estatisticas-esportivas.jar \
    --manifest build/MANIFEST.MF \
    -C build/classes .

echo "Compilacao concluida: dist/extrator-estatisticas-esportivas.jar"
