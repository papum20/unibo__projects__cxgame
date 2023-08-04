#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi

./lcompile

./ltest 9 9 5 pndb.alpha.Player pndb.nocells.Player -r 2 > debug/match/alpha_nocells_$1.txt

./ltest 9 9 5 pndb.alpha.Player pndb.nonum.Player -r 2 > debug/match/alpha_nonum_$1.txt

./ltest 9 9 5 pndb.nocells.Player pndb.nonum.Player -r 2 > debug/match/nocells_nonum_$1.txt
