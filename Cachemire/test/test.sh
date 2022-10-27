#!/bin/bash

if [ $# -ne 1 ]; then
    echo "passare un solo nome di file come argomento"
    exit -1
fi


OUT=$1
#OUT="/dev/null"

CACHEDIR=/tmp/cachemire

k=0
./cachemire_client localhost 13000 G:cachemire_client       >&      $OUT &
PID[k]=$! 
((k++))
./cachemire_client localhost 13000 G:cachemire_client  P:test.sh P:Makefile     2>&1 >> $OUT & 
PID[k]=$! 
((k++))
./cachemire_client localhost 13000 P:cachemire_client  G:test.sh     2>&1 >> $OUT & 
PID[k]=$! 
((k++))
./cachemire_client localhost 13000 G:cachemire_client       2>&1 >> $OUT & 
PID[k]=$! 
((k++))

touch $CACHEDIR/cachemire_client.flock

./cachemire_client localhost 13000 G:cachemire_client       2>&1 >> $OUT &
PID[k]=$! 
((k++))

for((i=0;i<k;++i)); do
    wait ${PID[i]}
done

