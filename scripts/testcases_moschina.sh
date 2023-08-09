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


./ltest 9 9 5		LXCONO.LXMARGIN.LXMARGIN pndb.alpha.nondb.Player				-r ${TESTS} > ${OUTDIR}/xcono_nondb_995.txt
cat scripts/testcases_moschina.sh >> ${OUTDIR}/xcono_nondb_995.txt
./ltest 18 20 7		LXCONO.LXMARGIN.LXMARGIN pndb.alpha.nondb.Player				-r ${TESTS} > ${OUTDIR}/xcono_nondb_18207.txt
cat scripts/testcases_moschina.sh >> ${OUTDIR}/xcono_nondb_18207.txt

./ltest 9 9 5		LXCONO.LXMARGIN.LXMARGIN pndb.alpha.orddb.Player				-r ${TESTS} > ${OUTDIR}/xcono_orddb_995.txt
cat scripts/testcases_moschina.sh >> ${OUTDIR}/xcono_orddb_995.txt
./ltest 18 20 7		LXCONO.LXMARGIN.LXMARGIN pndb.alpha.orddb.Player				-r ${TESTS} > ${OUTDIR}/xcono_orddb_18207.txt
cat scripts/testcases_moschina.sh >> ${OUTDIR}/xcono_orddb_18207.txt


#./ltest 9 9 5	pndb.alpha.Player 				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/alpha_ranch_995.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/alpha_ranch_995.txt
#./ltest 9 9 5	pndb.nocel.Player				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nocel_ranch_995.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/nocel_ranch_995.txt
#./ltest 9 9 5	pndb.nocel.nonmc.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nonmc_ranch_995.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/nonmc_ranch_995.txt
#./ltest 9 9 5	pndb.nocel.nonmc.tryit.Player	pndb.nocel.nonmc.tryitranch.Player	-r ${TESTS} > ${OUTDIR}/tryit_ranch_995.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/tryit_ranch_995.txt
#./ltest 9 9 5	pndb.alpha.nonum.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nonum_ranch_995.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/nonum_ranch_995.txt
#./ltest 9 9 5	pndb.alpha.halfn.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/halfn_ranch_995.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/halfn_ranch_995.txt
#
#./ltest 18 22 7	pndb.alpha.Player				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/alpha_ranch_18227.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/alpha_ranch_995.txt
#./ltest 18 22 7	pndb.nocel.Player				pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nocel_ranch_18227.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/nocel_ranch_995.txt
#./ltest 18 22 7	pndb.nocel.nonmc.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nonmc_ranch_18227.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/nonmc_ranch_995.txt
#./ltest 18 22 7	pndb.nocel.nonmc.tryit.Player	pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/tryit_ranch_18227.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/tryit_ranch_995.txt
#./ltest 18 22 7	pndb.alpha.nonum.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/nonum_ranch_18227.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/nonum_ranch_995.txt
#./ltest 18 22 7	pndb.alpha.halfn.Player			pndb.nocel.nonmc.tryit.ranch.Player	-r ${TESTS} > ${OUTDIR}/halfn_ranch_18227.txt
#cat ./scripts/testcases_moschina >> ${OUTDIR}/halfn_ranch_995.txt

