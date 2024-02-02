# unibo__projects__cxgame
CXGame - project for Algorithms course, at University of Bologna, year 2022/2023.  

## Commands

For executing, you can use the following (linux) commands:
*	`./lcompile` : create .class files in ./out/ : `javac -d out -sourcepath src src/connectx/pndb/*.java src/connectx/pndb/*/*.java`
*	`./lplay connectx.pndb.PnSearch [PARAMS]` : play human vs. player : `java -cp out connectx.CXGame pndb.PnSearch [PARAMS]`
*	specifying optional PARAMS for CXGame
*	`./ltest connectx.pndb.PnSearch PLAYER2 [PARAMS]` : play player vs. player : `java -cp out connectx.CXPlayerTester pndbPnSearch PLAYER2 [PARAMS]`
*	specifying PLAYER2 and optional PARAMS for CXPlayerTester
*	`./lclean` : remove all .class files : `rm -r out/*`

## Files

-	`doc/`: documentation (report.pdf)
-	`out/`: for compilation purposes
-	`src/`: source code (`connectx.pndb` package)
-	`*`: scripts
