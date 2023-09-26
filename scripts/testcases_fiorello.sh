#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/fiorello"
OUTDIR="${_OUTDIR}/$1"
TESTS="7"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


./ltest 9 9 5		pndb.delt2.PnSearch pndb.deltold.PnSearch		-r ${TESTS} > ${OUTDIR}/delt2_delto_995.txt
cat scripts/testcases_fiorello.sh >> ${OUTDIR}/delt2_delto_995.txt
./ltest 18 20 7		pndb.delt2.PnSearch pndb.deltold.PnSearch		-r ${TESTS} > ${OUTDIR}/delt2_delto_18207.txt
cat scripts/testcases_fiorello.sh >> ${OUTDIR}/delt2_delto_18207.txt



#./ltest 9 9 5		pndb.alpha.Player pndb.betha.Player				-r ${TESTS} > ${OUTDIR}/alpha_betha_995.txt
#cat scripts/testcases_fiorello.sh >> ${OUTDIR}/alpha_betha_995.txt
#./ltest 9 9 5		pndb.alpha.Player pndb.betha.scomb.Player		-r ${TESTS} > ${OUTDIR}/alpha_scomb_995.txt
#cat scripts/testcases_fiorello.sh >> ${OUTDIR}/alpha_scomb_995.txt
#
#./ltest 18 20 7		pndb.alpha.Player pndb.betha.Player				-r ${TESTS} > ${OUTDIR}/alpha_betha_18207.txt
#cat scripts/testcases_fiorello.sh >> ${OUTDIR}/alpha_scomb_18207.txt
#./ltest 18 20 7		pndb.alpha.Player pndb.betha.scomb.Player		-r ${TESTS} > ${OUTDIR}/alpha_scomb_18207.txt
#cat scripts/testcases_fiorello.sh >> ${OUTDIR}/alpha_scomb_18207.txt



##./ltest 9 9 5	pndb.alpha.Player pndb.nocel.nonmc.tryit.Player			-r ${TESTS} > ${OUTDIR}/alpha_tryit_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/alpha_tryit_995.txt
##./ltest 9 9 5	pndb.nocel.Player pndb.nocel.nonmc.tryit.Player			-r ${TESTS} > ${OUTDIR}/nocel_tryit_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/nocel_tryit_995.txt
##./ltest 9 9 5	pndb.nocel.nonmc.Player pndb.nocel.nonmc.tryit.Player	-r ${TESTS} > ${OUTDIR}/nonmc_tryit_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/nonmc_tryit_995.txt
##./ltest 9 9 5	pndb.alpha.nonum.Player pndb.nocel.nonmc.tryit.Player	-r ${TESTS} > ${OUTDIR}/nonum_tryit_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/nonum_tryit_995.txt
##
##./ltest 9 9 5	pndb.alpha.Player pndb.nocel.nonmc.tryit.ranch.Player			-r ${TESTS} > ${OUTDIR}/alpha_ranch_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/alpha_ranch_995.txt
##./ltest 9 9 5	pndb.nocel.Player pndb.nocel.nonmc.tryit.ranch.Player			-r ${TESTS} > ${OUTDIR}/nocel_ranch_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/nocel_ranch_995.txt
##./ltest 9 9 5	pndb.nocel.nonmc.Player pndb.nocel.nonmc.tryit.ranch.Player		-r ${TESTS} > ${OUTDIR}/nonmc_ranch_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/nonmc_ranch_995.txt
##./ltest 9 9 5	pndb.alpha.nonum.Player pndb.nocel.nonmc.tryit.ranch.Player		-r ${TESTS} > ${OUTDIR}/nonum_ranch_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/nonum_ranch_995.txt
##
##./ltest 9 9 5	pndb.nocel.nonmc.tryit.Player pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/tryit_ranch_995.txt
##cat testcases_debian.sh >> ${OUTDIR}/tryit_ranch_995.txt

