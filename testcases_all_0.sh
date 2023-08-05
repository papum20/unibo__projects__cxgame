#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/debian"
OUTDIR="${_OUTDIR}/$1"
TESTS="3"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}


./lcompile

./ltest 8 7 4		pndb.alpha.Player pndb.nocel.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nocel_874.txt
./ltest 15 15 7		pndb.alpha.Player pndb.nocel.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nocel_15.txt
./ltest 20 20 12	pndb.alpha.Player pndb.nocel.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nocel_2012.txt
./ltest 50 40 30	pndb.alpha.Player pndb.nocel.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nocel_504030.txt
./ltest 50 40 13	pndb.alpha.Player pndb.nocel.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nocel_504013.txt
./ltest 70 60 21	pndb.alpha.Player pndb.nocel.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nocel_706021.txt

./ltest 8 7 4		pndb.alpha.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonum_874.txt
./ltest 15 15 7		pndb.alpha.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonum_15.txt
./ltest 20 20 12	pndb.alpha.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonum_2012.txt
./ltest 50 40 30	pndb.alpha.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonum_504030.txt
./ltest 50 40 13	pndb.alpha.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonum_504013.txt
./ltest 70 60 21	pndb.alpha.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonum_706021.txt

./ltest 8 7 4		pndb.alpha.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonmc_874.txt
./ltest 15 15 7 	pndb.alpha.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonmc_15.txt
./ltest 20 20 12	pndb.alpha.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonmc_2012.txt
./ltest 50 40 30	pndb.alpha.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonmc_504030.txt
./ltest 50 40 13	pndb.alpha.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonmc_504013.txt
./ltest 70 60 21	pndb.alpha.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_nonmc_706021.txt

./ltest 8 7 4 		pndb.nocel.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonum_874.txt
./ltest 15 15 7 	pndb.nocel.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonum_15.txt
./ltest 20 20 12 	pndb.nocel.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonum_2012.txt
./ltest 50 40 30 	pndb.nocel.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonum_504030.txt
./ltest 50 40 13 	pndb.nocel.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonum_504013.txt
./ltest 70 60 21 	pndb.nocel.Player pndb.nonum.Player				-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonum_706021.txt

./ltest 8 7 4		pndb.nocel.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonmc_874.txt
./ltest 15 15 7 	pndb.nocel.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonmc_15.txt
./ltest 20 20 12	pndb.nocel.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonmc_2012.txt
./ltest 50 40 30	pndb.nocel.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonmc_504030.txt
./ltest 50 40 13	pndb.nocel.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonmc_504013.txt
./ltest 70 60 21	pndb.nocel.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_nonmc_706021.txt

./ltest 8 7 4		pndb.nonum.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_nonmc_874.txt
./ltest 15 15 7 	pndb.nonum.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_nonmc_15.txt
./ltest 20 20 12	pndb.nonum.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_nonmc_2012.txt
./ltest 50 40 30	pndb.nonum.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_nonmc_504030.txt
./ltest 50 40 13	pndb.nonum.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_nonmc_504013.txt
./ltest 70 60 21	pndb.nonum.Player pndb.nocel.nonmc.Player		-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_nonmc_706021.txt

