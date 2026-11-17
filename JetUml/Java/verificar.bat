@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\jetuml\Check.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp out jetuml.Check
