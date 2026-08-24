@echo off
setlocal
cd /d %~dp0

call compilar.bat
if errorlevel 1 exit /b 1

if not exist build\test-classes mkdir build\test-classes
dir /s /b src\test\java\*.java > build\fontes-testes.txt
call javac --release 17 -encoding UTF-8 -cp build\classes -d build\test-classes @build\fontes-testes.txt
if errorlevel 1 exit /b 1

call java -ea -cp "build\classes;build\test-classes" br.edu.linguagens.esportes.TodosOsTestes
endlocal
