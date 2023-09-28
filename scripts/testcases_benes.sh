#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/benes"
OUTDIR="${_OUTDIR}/$1"
TESTS="3"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile



./ltest 18 20 7		pndb.delta.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/delta_xcono_18207.txt
cat scripts/testcases_giovanna.sh >> ${OUTDIR}/delta_xcono_18207.txt

./ltest 9 9 5		pndb.delta.PnSearch LXCONO.LXMARGIN.LXMARGIN		-r ${TESTS} > ${OUTDIR}/delta_xcono_995.txt
cat scripts/testcases_giovanna.sh >> ${OUTDIR}/delta_xcono_995.txt



#./ltest 9 9 5		pndb.alpha.Player pndb.betha.Player				-r ${TESTS} > ${OUTDIR}/alpha_betha_995.txt
#cat scripts/testcases_benes.sh >> ${OUTDIR}/alpha_betha_995.txt
#./ltest 9 9 5		pndb.alpha.Player pndb.betha.scomb.Player		-r ${TESTS} > ${OUTDIR}/alpha_scomb_995.txt
#cat scripts/testcases_benes.sh >> ${OUTDIR}/alpha_scomb_995.txt
#
#./ltest 18 20 7		pndb.alpha.Player pndb.betha.Player				-r ${TESTS} > ${OUTDIR}/alpha_betha_18207.txt
#cat scripts/testcases_benes.sh >> ${OUTDIR}/alpha_scomb_18207.txt
#./ltest 18 20 7		pndb.alpha.Player pndb.betha.scomb.Player		-r ${TESTS} > ${OUTDIR}/alpha_scomb_18207.txt
#cat scripts/testcases_benes.sh >> ${OUTDIR}/alpha_scomb_18207.txt



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

