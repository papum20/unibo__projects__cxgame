#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/final1"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile



./ltest 9 9 5		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_995.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/delta_dnull_995.txt

./ltest 9 9 5		pndbtime.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 2 > ${OUTDIR}/pndbtime_xcono_995.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/pndbtime_xcono_995.txt


./ltest 18 20 7		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_18207.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/delta_dnull_18207.txt

./ltest 18 20 7		pndbtime.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 2 > ${OUTDIR}/pndbtime_xcono_18207.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/pndbtime_xcono_18207.txt



./ltest 9 9 5		pndbg.dnull.PnSearch pndbg.delta.PnSearch 		-r 1 > ${OUTDIR}/dnull_delta_995.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/dnull_delta_995.txt

./ltest 9 9 5		LXCONO.LXMARGIN.LXMARGIN pndbtime.PnSearch 		-r 1 > ${OUTDIR}/xcono_pndbtime_995.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/xcono_pndbtime_995.txt


./ltest 18 20 7		pndbg.dnull.PnSearch pndbg.delta.PnSearch 		-r 1 > ${OUTDIR}/dnull_delta_18207.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/dnull_delta_18207.txt

./ltest 18 20 7		LXCONO.LXMARGIN.LXMARGIN pndbtime.PnSearch		-r 1 > ${OUTDIR}/xcono_pndbtime_18207.txt
cat scripts/testcases_final1.sh >> ${OUTDIR}/xcono_pndbtime_18207.txt
