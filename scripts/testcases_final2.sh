#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/final2"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


# for data

./ltest 38 58 12		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_385812.txt
cat scripts/testcases_final2.sh >> ${OUTDIR}/delta_dnull_385812.txt

# final test (except some dbg is on)

./ltest 38 58 12		pndbtime.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 2 > ${OUTDIR}/pndbtime_xcono_385812.txt
cat scripts/testcases_final2.sh >> ${OUTDIR}/pndbtime_xcono_385812.txt

./ltest 70 47 11		pndbtime.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 2 > ${OUTDIR}/pndbtime_xcono_704711.txt
cat scripts/testcases_final2.sh >> ${OUTDIR}/pndbtime_xcono_704711.txt

./ltest 100 100 30		pndbtime.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 2 > ${OUTDIR}/pndbtime_xcono_10010030.txt
cat scripts/testcases_final2.sh >> ${OUTDIR}/pndbtime_xcono_10010030.txt


# secondary

./ltest 70 47 11		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_704711.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_704711.txt

./ltest 100 100 30		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_10010030.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_10010030.txt



