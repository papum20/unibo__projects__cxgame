#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/dnull"
OUTDIR="${_OUTDIR}/$1"
TESTS="1"

PLAYER_NAME_1_1=delta
PLAYER_NAME_1_2=l0l0l
PLAYER_NAME_2_1=dnull
PLAYER_NAME_2_2=l0l0l
PLAYER_NAME_3_1=delta
PLAYER_NAME_3_2=l1l1l
PLAYER_NAME_4_1=dnull
PLAYER_NAME_4_2=l1l1l

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


# delta l0
./ltest 9 9 5		pndb.delta.PnSearch connectx.L0.L0		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_1_1}_${PLAYER_NAME_1_2}_995.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_1_1}_${PLAYER_NAME_1_2}_995.txt

./ltest 18 20 7		pndb.delta.PnSearch connectx.L0.L0		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_1_1}_${PLAYER_NAME_1_2}.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_1_1}_${PLAYER_NAME_1_2}_18207.txt

#dnull l0
./ltest 9 9 5		pndb.dnull.PnSearch connectx.L0.L0		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_2_1}_${PLAYER_NAME_2_2}_995.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_2_1}_${PLAYER_NAME_2_2}_995.txt

./ltest 18 20 7		pndb.dnull.PnSearch connectx.L0.L0		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_2_1}_${PLAYER_NAME_2_2}.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_2_1}_${PLAYER_NAME_2_2}_18207.txt

#delta l1
./ltest 9 9 5		pndb.delta.PnSearch connectx.L1.L1		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_3_1}_${PLAYER_NAME_3_2}_995.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_3_1}_${PLAYER_NAME_3_2}_995.txt

./ltest 18 20 7		pndb.delta.PnSearch connectx.L1.L1		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_3_1}_${PLAYER_NAME_3_2}.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_3_1}_${PLAYER_NAME_3_2}_18207.txt

#dnull l1
./ltest 9 9 5		pndb.dnull.PnSearch connectx.L1.L1		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_4_1}_${PLAYER_NAME_4_2}_995.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_4_1}_${PLAYER_NAME_4_2}_995.txt

./ltest 18 20 7		pndb.dnull.PnSearch connectx.L1.L1		-r ${TESTS} > ${OUTDIR}/${PLAYER_NAME_4_1}_${PLAYER_NAME_4_2}.txt
cat scripts/testcases_dnull_pippa.sh >> ${OUTDIR}/${PLAYER_NAME_4_1}_${PLAYER_NAME_4_2}_18207.txt


