# unibo__projects__cxgame
CXGame - project for Algorithms course, at University of Bologna, year 2022/2023.  
It consists of an implementation of a player for the Connect-x game, implemented as a Proof-number search (PN-search) using a Dependency-based search (DB-search) as evaluation for a game-tree node.  

## Commands

For executing, you can use the following (linux) commands:
*	`./lcompile` : create .class files in ./out/ : `javac -d out -sourcepath src src/connectx/pndb/*.java src/connectx/pndb/*/*.java`
*	`./lplay connectx.pndb.PnSearch [PARAMS]` : play human vs. player : `java -cp out connectx.CXGame pndb.PnSearch [PARAMS]`
*	specifying optional PARAMS for CXGame
*	`./ltest connectx.pndb.PnSearch PLAYER2 [PARAMS]` : play player vs. player : `java -cp out connectx.CXPlayerTester pndbPnSearch PLAYER2 [PARAMS]`
*	specifying PLAYER2 and optional PARAMS for CXPlayerTester
*	`./lclean` : remove all .class files : `rm -r out/*`

## Files

-	`debug/`: where debug files are put
	-	`debug/charts`: files used to create charts
-	`doc/`: documentation
	-	`doc/img/`: imgs and charts (for report)
	-	`doc/report.pdf`: report (in italian)
	-	`doc/tests.md`: results of some tests
-	`out/`: for compilation purposes
-	`scripts/`: testcases scripts for testing
-	`src/`: source code
	-	`src/connectx/` : provided project's request
	-	`src/pndb/` : implementation of my player
	-	`src/pndbg/` : old implementations of my player
	-	`src/pndbtime/` : another old implementation of my player (actually almost the same as pndb)
-	`*`: scripts

## References

Explainatory report (in italian) in `doc/report.pdf`.  

Inspired by "Searching for Solutions in Games and Artifcial Intelligence" by Victor Allis, 1994 (http://fragrieu.free.fr/SearchingForSolutions.pdf).  