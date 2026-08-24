@echo off
setlocal
cd /d %~dp0

if exist build rmdir /s /q build
if not exist build\classes mkdir build\classes
if not exist dist mkdir dist

dir /s /b src\main\java\*.java > build\fontes-principais.txt
call javac --release 17 -encoding UTF-8 -d build\classes @build\fontes-principais.txt
if errorlevel 1 exit /b 1

xcopy /e /i /y src\main\resources\* build\classes\ >nul
(
  echo Manifest-Version: 1.0
  echo Main-Class: br.edu.linguagens.esportes.Aplicacao
  echo Implementation-Title: Extrator de Estatisticas Esportivas
  echo Implementation-Version: 1.0.0
  echo.
) > build\MANIFEST.MF

call jar cfm dist\extrator-estatisticas-esportivas.jar build\MANIFEST.MF -C build\classes .
if errorlevel 1 exit /b 1

echo Compilacao concluida: dist\extrator-estatisticas-esportivas.jar
endlocal
