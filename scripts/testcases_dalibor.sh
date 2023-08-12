#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/dalibor"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile



./ltest 9 9 5		pndb.alpha.Player pndb.gamma.Player				-r ${TESTS} > ${OUTDIR}/alpha_gamma_995.txt
cat scripts/testcases_dalibor.sh >> ${OUTDIR}/alpha_gamma_995.txt
./ltest 18 20 7		pndb.alpha.Player pndb.gamma.Player				-r ${TESTS} > ${OUTDIR}/alpha_gamma_18207.txt
cat scripts/testcases_dalibor.sh >> ${OUTDIR}/alpha_gamma_18207.txt

./ltest 9 9 5		LXCONO.LXMARGIN.LXMARGIN pndb.gamma.Player				-r ${TESTS} > ${OUTDIR}/xcono_gamma_995.txt
cat scripts/testcases_dalibor.sh >> ${OUTDIR}/xcono_gamma_995.txt
./ltest 18 20 7		LXCONO.LXMARGIN.LXMARGIN pndb.gamma.Player				-r ${TESTS} > ${OUTDIR}/xcono_gamma_18207.txt
cat scripts/testcases_dalibor.sh >> ${OUTDIR}/xcono_gamma_18207.txt




#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.nocel.Player				-r ${TESTS} > ${OUTDIR}/ranch_nocel_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nocel_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.nocel.nonmc.Player			-r ${TESTS} > ${OUTDIR}/ranch_nonmc_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nonmc_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.nocel.nonmc.tryit.Player	-r ${TESTS} > ${OUTDIR}/ranch_tryit_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_tryit_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.nonmc.Player				-r ${TESTS} > ${OUTDIR}/ranch_nomc0_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nomc0_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.nonmc.tryit.Player			-r ${TESTS} > ${OUTDIR}/ranch_tryt0_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_tryt0_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/ranch_rnch0_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_rnch0_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.alpha.Player				-r ${TESTS} > ${OUTDIR}/ranch_alpha_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_alpha_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.alpha.nonum.Player			-r ${TESTS} > ${OUTDIR}/ranch_nonum_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nonum_995.txt
#./ltest 9 9 5		pndb.nocel.nonmc.tryit.ranch.Player pndb.alpha.halfn.Player			-r ${TESTS} > ${OUTDIR}/ranch_halfn_995.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_halfn_995.txt
#
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.nocel.Player				-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_nocel_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nocel_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.nocel.nonmc.Player			-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_nonmc_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nonmc_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.nocel.nonmc.tryit.Player	-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_tryit_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_tryit_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.nonmc.Player				-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_nomc0_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nomc0_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.nonmc.tryit.Player			-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_tryt0_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_tryt0_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.nonmc.tryit.ranch.Player	-r ${TESTS} | tail -40 > ${OUTDIR}ranch_rnch0_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_rnch0_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.alpha.Player				-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_alpha_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_alpha_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.alpha.nonum.Player			-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_nonum_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_nonum_18147.txt
#./ltest 18 14 7		pndb.nocel.nonmc.tryit.ranch.Player pndb.alpha.halfn.Player			-r ${TESTS} | tail -40 > ${OUTDIR}/ranch_halfn_18147.txt
#cat scripts/testcases_dalibor.sh >> ${OUTDIR}/ranch_halfn_18147.txt
