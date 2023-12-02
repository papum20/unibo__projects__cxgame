#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/dnull"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile



./ltest 9 9 5		pndb.delta.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/delta_xcono_995.txt
cat scripts/testcases_dnull_cono.sh >> ${OUTDIR}/delta_xcono_995.txt

./ltest 18 20 7		pndb.delta.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/delta_xcono_18207.txt
cat scripts/testcases_dnull_cono.sh >> ${OUTDIR}/delta_xcono_18207.txt

./ltest 38 58 12	pndb.delta.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/delta_xcono_385812.txt
cat scripts/testcases_dnull_cono.sh >> ${OUTDIR}/delta_xcono_385812.txt

./ltest 9 9 5		pndb.dnull.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/dnull_xcono_995.txt
cat scripts/testcases_dnull_cono.sh >> ${OUTDIR}/dnull_xcono_995.txt

./ltest 18 20 7		pndb.dnull.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/dnull_xcono_18207.txt
cat scripts/testcases_dnull_cono.sh >> ${OUTDIR}/dnull_xcono_18207.txt

./ltest 38 58 12	pndb.dnull.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/dnull_xcono_385812.txt
cat scripts/testcases_dnull_cono.sh >> ${OUTDIR}/dnull_xcono_385812.txt


