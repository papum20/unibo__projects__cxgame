#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing arg"
	exit 1
fi

./lcompile

./ltest 9 9 5 connectx.L1.L1 pndb.alpha.Player -r 2 > debug/match/pndb_L1_$1.txt

./ltest 9 9 5 connectx.L1.L1 pndb.nocells.Player -r 2 > debug/match/pndb2_L1_$1.txt

./ltest 9 9 5 pndb.alpha.Player pndb.nocells.Player -r 5 > debug/match/pndb_pndb2_$1.txt
