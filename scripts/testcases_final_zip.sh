#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/final_zip"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile.sh


./ltest.sh 9 9 5		connectx.pndb.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 1 > ${OUTDIR}/pndb_xcono_995.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/pndb_xcono_995.txt

./ltest.sh 9 9 5		connectx.pndb.PnSearch pndbg.delta.PnSearch			-r 1 > ${OUTDIR}/pndb_delta_995.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/pndb_delta_995.txt

./ltest.sh 20 18 7		 LXCONO.LXMARGIN.LXMARGIN connectx.pndb.PnSearch	-r 1 > ${OUTDIR}/xcono_pndb_20187.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/xcono_pndb_20187.txt

./ltest.sh 20 18 7		pndb.delta.PnSearch connectx.pndb.PnSearch			-r 1 > ${OUTDIR}/delta_pndb_20187.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/delta_pndb_20187.txt

./ltest.sh 38 58 12		connectx.pndb.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r 1 > ${OUTDIR}/pndb_xcono_385812.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/pndb_xcono_385812.txt

./ltest.sh 38 58 12		connectx.pndb.PnSearch pndb.delta.PnSearch			-r 1 > ${OUTDIR}/pndb_delta_385812.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/pndb_delta_385812.txt

./ltest.sh 70 47 11		LXCONO.LXMARGIN.LXMARGIN connectx.pndb.PnSearch		-r 1 > ${OUTDIR}/xcono_pndb_704711.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/xcono_pndb_704711.txt

./ltest.sh 70 47 11		pndb.delta.PnSearch connectx.pndb.PnSearch			-r 1 > ${OUTDIR}/delta_pndb_704711.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/delta_pndb_704711.txt

./ltest.sh 100 100 30		connectx.pndb.PnSearch LXCONO.LXMARGIN.LXMARGIN	-r 1 > ${OUTDIR}/pndb_xcono_10010030.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/pndb_xcono_10010030.txt

./ltest.sh 100 100 30		connectx.pndb.PnSearch pndb.delta.PnSearch		-r 1 > ${OUTDIR}/pndb_delta_10010030.txt
cat scripts/testcases_final_zip.sh >> ${OUTDIR}/pndb_delta_10010030.txt


