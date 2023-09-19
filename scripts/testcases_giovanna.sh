#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/giovanna"
OUTDIR="${_OUTDIR}/$1"
TESTS="3"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


./ltest 9 9 5		pndb.delta.Player LXCONO.LXMARGIN.LXMARGIN				-r ${TESTS} > ${OUTDIR}/delta_xcono_995.txt
cat scripts/testcases_giovanna.sh >> ${OUTDIR}/delta_xcono_995.txt
./ltest 18 20 7		pndb.delta.Player LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/delta_xcono_18207.txt
cat scripts/testcases_giovanna.sh >> ${OUTDIR}/delta_xcono_18207.txt




