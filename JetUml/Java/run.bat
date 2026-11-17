@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\jetuml\JetUml.java
if errorlevel 1 exit /b 1
java -cp out jetuml.JetUml
