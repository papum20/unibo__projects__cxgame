#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/debian"
OUTDIR="${_OUTDIR}/$1"
TESTS="5"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile

./ltest 9 9 5	pndb.alpha.Player pndb.nocel.nonmc.tryit.Player			-r ${TESTS} > ${OUTDIR}/alpha_tryit_995.txt
cat testcases_debian.sh >> ${OUTDIR}/alpha_tryit_995.txt
./ltest 9 9 5	pndb.nocel.Player pndb.nocel.nonmc.tryit.Player			-r ${TESTS} > ${OUTDIR}/nocel_tryit_995.txt
cat testcases_debian.sh >> ${OUTDIR}/nocel_tryit_995.txt
./ltest 9 9 5	pndb.nocel.nonmc.Player pndb.nocel.nonmc.tryit.Player	-r ${TESTS} > ${OUTDIR}/nonmc_tryit_995.txt
cat testcases_debian.sh >> ${OUTDIR}/nonmc_tryit_995.txt
./ltest 9 9 5	pndb.alpha.nonum.Player pndb.nocel.nonmc.tryit.Player	-r ${TESTS} > ${OUTDIR}/nonum_tryit_995.txt
cat testcases_debian.sh >> ${OUTDIR}/nonum_tryit_995.txt

./ltest 9 9 5	pndb.alpha.Player pndb.nocel.nonmc.tryit.ranch.Player			-r ${TESTS} > ${OUTDIR}/alpha_ranch_995.txt
cat testcases_debian.sh >> ${OUTDIR}/alpha_ranch_995.txt
./ltest 9 9 5	pndb.nocel.Player pndb.nocel.nonmc.tryit.ranch.Player			-r ${TESTS} > ${OUTDIR}/nocel_ranch_995.txt
cat testcases_debian.sh >> ${OUTDIR}/nocel_ranch_995.txt
./ltest 9 9 5	pndb.nocel.nonmc.Player pndb.nocel.nonmc.tryit.ranch.Player		-r ${TESTS} > ${OUTDIR}/nonmc_ranch_995.txt
cat testcases_debian.sh >> ${OUTDIR}/nonmc_ranch_995.txt
./ltest 9 9 5	pndb.alpha.nonum.Player pndb.nocel.nonmc.tryit.ranch.Player		-r ${TESTS} > ${OUTDIR}/nonum_ranch_995.txt
cat testcases_debian.sh >> ${OUTDIR}/nonum_ranch_995.txt

./ltest 9 9 5	pndb.nocel.nonmc.tryit.Player pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/tryit_ranch_995.txt
cat testcases_debian.sh >> ${OUTDIR}/tryit_ranch_995.txt

