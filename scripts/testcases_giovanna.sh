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


./ltest 9 9 5		LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.ranch.Player		-r ${TESTS} > ${OUTDIR}/xcono_ranch_995.txt
cat ./scripts/testcases_giovanna.sh >> ${OUTDIR}/xcono_ranch_995.txt
./ltest 20 20 7		LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.ranch.Player		-r ${TESTS} > ${OUTDIR}/xcono_ranch_20207.txt
cat ./scripts/testcases_giovanna.sh >> ${OUTDIR}/xcono_ranch_20207.txt
./ltest 50 50 14	LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.ranch.Player		-r 3 | tail -4 > ${OUTDIR}/xcono_ranch_505014.txt
cat ./scripts/testcases_giovanna.sh >> ${OUTDIR}/xcono_ranch_505014.txt
./ltest 50 50 7		LXCONO.LXMARGIN.LXMARGIN pndb.nocel.nonmc.tryit.ranch.Player		-r 3 | tail -4 > ${OUTDIR}/xcono_ranch_50507.txt
cat ./scripts/testcases_giovanna.sh >> ${OUTDIR}/xcono_ranch_50507.txt
