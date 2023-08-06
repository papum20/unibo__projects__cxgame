::COMPILER FOR A JAVA PROJECT
::TO BE CALLED IN MAIN/ROOT FOLDER

@echo off
title MAKE

setlocal
set argsEmpty=1
set curPath="src"

for %%x in (%*) do set argsEmpty=0
IF %argsEmpty%==0 set curPath=%1

FOR /D %%G in ("%curPath%\*") DO (
	call ./wcompile %%G
)

echo %1 %curPath%
javac -d out -sourcepath src %curPath%\*.java