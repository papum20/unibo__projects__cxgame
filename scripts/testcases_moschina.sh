#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/moschina"
OUTDIR="${_OUTDIR}/$1"
TESTS="4"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile

./ltest 9 9 5	pndb.alpha.Player 				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/alpha_ranch_995.txt
cat ./scripts/testcases_moschina >> alpha_ranch_995.txt
./ltest 9 9 5	pndb.nocel.Player				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nocel_ranch_995.txt
cat ./scripts/testcases_moschina >> nocel_ranch_995.txt
./ltest 9 9 5	pndb.nocel.nonmc.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nonmc_ranch_995.txt
cat ./scripts/testcases_moschina >> nonmc_ranch_995.txt
./ltest 9 9 5	pndb.nocel.nonmc.tryit.Player	pndb.nocel.nonmc.tryitranch.Player	-r ${TESTS} > ${OUTDIR}/tryit_ranch_995.txt
cat ./scripts/testcases_moschina >> tryit_ranch_995.txt
./ltest 9 9 5	pndb.alpha.nonum.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nonum_ranch_995.txt
cat ./scripts/testcases_moschina >> nonum_ranch_995.txt
./ltest 9 9 5	pndb.alpha.halfn.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/halfn_ranch_995.txt
cat ./scripts/testcases_moschina >> halfn_ranch_995.txt

./ltest 18 22 7	pndb.alpha.Player				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/alpha_ranch_18227.txt
cat ./scripts/testcases_moschina >> alpha_ranch_995.txt
./ltest 18 22 7	pndb.nocel.Player				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/nocel_ranch_18227.txt
cat ./scripts/testcases_moschina >> nocel_ranch_995.txt
./ltest 18 22 7	pndb.nocel.nonmc.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/nonmc_ranch_18227.txt
cat ./scripts/testcases_moschina >> nonmc_ranch_995.txt
./ltest 18 22 7	pndb.nocel.nonmc.tryit.Player	pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/tryit_ranch_18227.txt
cat ./scripts/testcases_moschina >> tryit_ranch_995.txt
./ltest 18 22 7	pndb.alpha.nonum.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/nonum_ranch_18227.txt
cat ./scripts/testcases_moschina >> nonum_ranch_995.txt
./ltest 18 22 7	pndb.alpha.halfn.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/halfn_ranch_18227.txt
cat ./scripts/testcases_moschina >> halfn_ranch_995.txt

