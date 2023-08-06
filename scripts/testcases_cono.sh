#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/dalibor"
OUTDIR="${_OUTDIR}/$1"
TESTS="3"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


./ltest 9 9 5	pndb.alpha.Player LXCONO.LXMARGIN.LXMARGIN					-r ${TESTS} > ${OUTDIR}/alpha_conom_995.txt
./ltest 9 9 5	pndb.nocel.Player LXCONO.LXMARGIN.LXMARGN					-r ${TESTS} > ${OUTDIR}/nocel_conom_995.txt
./ltest 9 9 5	pndb.nocel.nonmc.Player LXCONO.LXMARGIN.LXMARGIN			-r ${TESTS} > ${OUTDIR}/nonmc_conom_995.txt
./ltest 9 9 5	pndb.nonum.Player LXCONO.LXMARGIN.LXMARGIN					-r ${TESTS} > ${OUTDIR}/nonum_conom_995.txt
./ltest 9 9 5	pndb.nocel.nonmc.tryit.Player LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/tryit_conom_995.txt


./ltest 20 20 7	pndb.alpha.Player LXCONO.LXMARGIN.LXMARGIN					-r ${TESTS} | tail -4 > ${OUTDIR}/alpha_conom_995.txt
./ltest 20 20 7	pndb.nocel.Player LXCONO.LXMARGIN.LXMARGN					-r ${TESTS} | tail -4 > ${OUTDIR}/nocel_conom_995.txt
./ltest 20 20 7	pndb.nocel.nonmc.Player LXCONO.LXMARGIN.LXMARGIN			-r ${TESTS} | tail -4 > ${OUTDIR}/nonmc_conom_995.txt
./ltest 20 20 7	pndb.nonum.Player LXCONO.LXMARGIN.LXMARGIN					-r ${TESTS} | tail -4 > ${OUTDIR}/nonum_conom_995.txt
./ltest 20 20 7	pndb.nocel.nonum.tryit.Player LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} | tail -4 > ${OUTDIR}/tryit_conom_995.txt

