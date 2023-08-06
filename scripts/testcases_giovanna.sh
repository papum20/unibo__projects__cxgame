#!/bin/bash

if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


_OUTDIR="debug/match/giovanna"
OUTDIR="${_OUTDIR}/$1"
TESTS="5"

mkdir ${_OUTDIR}
mkdir ${OUTDIR}

./lcompile


./ltest 9 9 5		LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.Player		-r ${TESTS} > ${OUTDIR}/xcono_tryit_995.txt
cat testcases_giovanna.sh >> ${OUTDIR}/xcono_tryit_995.txt
./ltest 20 20 7		LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.Player		-r ${TESTS} > ${OUTDIR}/xcono_tryit_20207.txt
cat testcases_giovanna.sh >> ${OUTDIR}/xcono_tryit_20207.txt
./ltest 50 50 14	LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.Player		-r 3 | tail -4 > ${OUTDIR}/xcono_tryit_505014.txt
cat testcases_giovanna.sh >> ${OUTDIR}/xcono_tryit_505014.txt
./ltest 50 50 7		LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.Player		-r 3 | tail -4 > ${OUTDIR}/xcono_tryit_50507.txt
cat testcases_giovanna.sh >> ${OUTDIR}/xcono_tryit_50507.txt

