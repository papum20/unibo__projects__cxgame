#!/bin/bash

SCORELXX=0
SCORELXImo=0
TOTPAREGGI=0

while IFS="\n" read linea
do

inizio=$(echo $linea | cut --delimiter=' ' --fields=1)

if [[ $inizio == "$1" ]]
then
	plus=$(echo $linea | cut --delimiter=' ' --fields=6)
	SCORELXImo=$(($SCORELXImo + $plus))
#	echo $SCORELXImo
fi

if [[ $inizio == "$2" ]]
then
        plus=$(echo $linea | cut --delimiter=' ' --fields=6)
        SCORELXX=$(($SCORELXX + $plus))
fi

pareggio=$(echo $linea | cut --delimiter=' ' --fields=10)
TOTPAREGGI=$(($TOTPAREGGI + $pareggio))
#echo $plus
done < <(cat $3 | grep Won)

date | tee -a /home/tiz/RisultatiParsati.txt
echo | tee -a /home/tiz/RisultatiParsati.txt
echo "il numero di vittorie di $1 è $SCORELXImo" | tee -a /home/tiz/RisultatiParsati.txt
echo | tee -a /home/tiz/RisultatiParsati.txt
echo "il numero di vittore di $2 è $SCORELXX" | tee -a /home/tiz/RisultatiParsati.txt
echo | tee -a /home/tiz/RisultatiParsati.txt
echo "il numero di pareggi è $TOTPAREGGI" | tee -a /home/tiz/RisultatiParsati.txt
echo | tee -a /home/tiz/RisultatiParsati.txt
echo | tee -a /home/tiz/RisultatiParsati.txt
