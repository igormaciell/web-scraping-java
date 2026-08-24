@echo off
setlocal
cd /d %~dp0

if not exist dist\extrator-estatisticas-esportivas.jar (
  call compilar.bat
  if errorlevel 1 exit /b 1
)

java -jar dist\extrator-estatisticas-esportivas.jar %*
endlocal
