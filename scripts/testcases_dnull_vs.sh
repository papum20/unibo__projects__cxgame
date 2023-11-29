#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/dnlvs"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile



./ltest 9 9 5		pndb.delta.PnSearch pndb.dnull.PnSearch			-r ${TESTS} > ${OUTDIR}/delta_dnull_995.txt
cat scripts/testcases_dnull_vs.sh >> ${OUTDIR}/delta_dnull_995.txt

./ltest 18 20 7		pndb.delta.PnSearch pndb.dnull.PnSearch			-r ${TESTS} > ${OUTDIR}/delta_dnull_18207.txt
cat scripts/testcases_dnull_vs.sh >> ${OUTDIR}/delta_dnull_18207.txt

./ltest 38 58 12	pndb.delta.PnSearch pndb.dnull.PnSearch			-r ${TESTS} > ${OUTDIR}/delta_dnull_385812.txt
cat scripts/testcases_dnull_vs.sh >> ${OUTDIR}/delta_dnull_385812.txt

./ltest 70 47 11	pndb.delta.PnSearch pndb.dnull.PnSearch			-r ${TESTS} > ${OUTDIR}/delta_dnull_704711.txt
cat scripts/testcases_dnull_vs.sh >> ${OUTDIR}/delta_dnull_704711.txt



