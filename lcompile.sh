#!/bin/bash

javac -d out -sourcepath src	\
	src/connectx/*.java	src/connectx/*/*.java	\
	src/pndb/*.java src/pndb/*/*.java	\
	src/pndbg/*/*.java src/pndbg/*/*/*.java src/pndbg/*/*/*/*.java src/pndbg/*/*/*/*/*.java src/pndbg/*/*/*/*/*.java	\
	src/LXCONO/LXMARGIN/*.java	\
	src/pndbtime/*.java src/pndbtime/*/*.java
	
#	src/pndb2/*.java src/pndb2/*/*.java	\
