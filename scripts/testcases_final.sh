#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/final"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


# pndb2 dnull: 9,9,5*7 - 18,20,7*7 - 38,58,12*3 - 70,47,11*2 - 100,100,30*2
# delta dnull: 9,9,5*7 - 18,20,7*7 - 38,58,12*3 - 70,47,11*2 - 100,100,30*2
# pndb2 xcono: 9,9,5*7 - 18,20,7*7 - 38,58,12*3 - 70,47,11*2 - 100,100,30*2

# pndb pndb2: 9,9,5*3 - 18,20,7*3
# pndb pndbtime: 18,20,7*3
# pndb2 alpha: 9,9,5*3 - 18,20,7*3
# pndb2 betha: 9,9,5*3 - 18,20,7*3



./ltest 9 9 5		pndb2.PnSearch pndbg.dnull.PnSearch				-r 7 > ${OUTDIR}/pndb2_dnull_995.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_dnull_995.txt

./ltest 9 9 5		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 7 > ${OUTDIR}/delta_dnull_995.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_995.txt

./ltest 9 9 5		pndb2.PnSearch LXCONO.LXMARGIN.LXMARGIN			-r 7 > ${OUTDIR}/pndb2_xcono_995.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_xcono_995.txt

./ltest 9 9 5		pndb.PnSearch pndb2.PnSearch					-r 3 > ${OUTDIR}/pndb_pndb2_995.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb_pndb2_995.txt

./ltest 9 9 5		pndb2.PnSearch pndbg.alpha.Player				-r 3 > ${OUTDIR}/pndb2_alpha_995.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_alpha_995.txt

./ltest 9 9 5		pndb2.PnSearch pndbg.betha.Player				-r 3 > ${OUTDIR}/pndb2_betha_995.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_betha_995.txt


./ltest 18 20 7		pndb2.PnSearch pndbg.dnull.PnSearch				-r 7 > ${OUTDIR}/pndb2_dnull_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_dnull_18207.txt

./ltest 18 20 7		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 7 > ${OUTDIR}/delta_dnull_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_18207.txt

./ltest 18 20 7		pndb2.PnSearch LXCONO.LXMARGIN.LXMARGIN			-r 7 > ${OUTDIR}/pndb2_xcono_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_xcono_18207.txt

./ltest 18 20 7		pndb.PnSearch pndb2.PnSearch					-r 3 > ${OUTDIR}/pndb_pndb2_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb_pndb2_18207.txt

./ltest 18 20 7		pndb.PnSearch pndbtime.PnSearch					-r 3 > ${OUTDIR}/pndb_pndbtime_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb_pndbtime_18207.txt

./ltest 18 20 7		pndb2.PnSearch pndbg.alpha.Player				-r 3 > ${OUTDIR}/pndb2_alpha_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_alpha_18207.txt

./ltest 18 20 7		pndb2.PnSearch pndbg.betha.Player				-r 3 > ${OUTDIR}/pndb2_betha_18207.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_betha_18207.txt


./ltest 38 58 12		pndb2.PnSearch pndbg.dnull.PnSearch				-r 3 > ${OUTDIR}/pndb2_dnull_385812.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_dnull_385812.txt

# fixed isWinningMove here
./ltest 38 58 12		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 3 > ${OUTDIR}/delta_dnull_385812.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_385812.txt

./ltest 38 58 12		pndb2.PnSearch LXCONO.LXMARGIN.LXMARGIN			-r 3 > ${OUTDIR}/pndb2_xcono_385812.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_xcono_385812.txt


./ltest 70 47 11		pndb2.PnSearch pndbg.dnull.PnSearch				-r 2 > ${OUTDIR}/pndb2_dnull_704711.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_dnull_704711.txt

./ltest 70 47 11		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_704711.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_704711.txt

./ltest 70 47 11		pndb2.PnSearch LXCONO.LXMARGIN.LXMARGIN			-r 2 > ${OUTDIR}/pndb2_xcono_704711.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_xcono_704711.txt


./ltest 100 100 30		pndb2.PnSearch pndbg.dnull.PnSearch				-r 2 > ${OUTDIR}/pndb2_dnull_10010030.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_dnull_10010030.txt

./ltest 100 100 30		pndbg.delta.PnSearch pndbg.dnull.PnSearch		-r 2 > ${OUTDIR}/delta_dnull_10010030.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/delta_dnull_10010030.txt

./ltest 100 100 30		pndb2.PnSearch LXCONO.LXMARGIN.LXMARGIN			-r 2 > ${OUTDIR}/pndb2_xcono_10010030.txt
cat scripts/testcases_final.sh >> ${OUTDIR}/pndb2_xcono_10010030.txt



