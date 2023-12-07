if [[ $# < 1 ]]
then
	echo "missing args"
	exit 1
fi


./scripts/testcases_dnull_vs.sh $1
./scripts/testcases_dnull_cono.sh $1