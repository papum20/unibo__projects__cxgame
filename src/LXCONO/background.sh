#!/bin/bash
for i in {1..10}
do
	java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.LXInomove.LXInomove connectx.LXX.LXX -v -t 10 -r 5 >>risultatonomoves.txt &
done
