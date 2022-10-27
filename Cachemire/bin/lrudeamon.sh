#!/bin/bash
#
#
# Author: Massimo Torquati (massimo.torquati@unipi.it)
# 
#
#
#

dirname=
cachesize=         
freefactor=0.05     # free space we want to maintain
maxremove=10        # default maximum number of files that will be removed
doremove=0          # file remove is turned off by default
rminteractive=""    # file removing is not interactive by default
verbose=0           # verbose mode disabled

usage() {
    echo
    echo "usage:"
    echo " $0 -d dirname -s cachesize [-f freefactor]  [-m maxremove] [-r] [-v] [-i] [-h]"
    echo
    echo " -d dirname (--dirname=<dir>)"
    echo "    cache directory  (mandatory argument, no default value)"
    echo
    echo " -s cachesize (--cachesize=<size>)"
    echo "    maximum cache size in MB (mandatory argoment, no default value)"
    echo
    echo " -f freefactor (--factor=<val>)"
    echo "    fraction of the cache size we want to keep free (optional argument, default $freefactor)"
    echo
    echo " -m maxremove (--maxremove=<val>)"
    echo "    max number of files that will be removed if the free space is below the freefactor"
    echo "    (optional argument, default $maxremove)"
    echo	
    echo " -r (--doremove)"
    echo "    remove files instead of just printing them (optional argument). For controlled remove use also option '-i'."
    echo
    echo " -i (--interactive)"
    echo "    interactive remove (i.e., rm -i) (optional argument)"
    echo
    echo " -v (--verbose)"
    echo "    verbose mode (optional argument)"
    echo
    echo " -h (--help)"
    echo "    usage message (optional argument)"
    echo 
}

# ----------------- 


if [[ "$#" == 0 ]]
then
    echo "No argument provided!"
    usage
    exit 1
fi

# checking arguments
while [[ "$#" > 0 ]]; do
  case "$1" in
    -d) dirname="$2";       shift 2;;
    -s) cachesize="$2";     shift 2;;
    -f) freefactor="$2";    shift 2;;
    -m) maxremove="$2";     shift 2;;
    -r) doremove=1;         shift 1;;
    -i) rminteractive="-i"; shift 1;;
    -v) verbose=1;          shift 1;;
    -h) usage; exit 0;;

    # long arguments
    --dirname=*)   dirname="${1#*=}";    shift 1;;
    --cachesize=*) cachesize="${1#*=}";  shift 1;;
    --factor=*)    freefactor="${1#*=}"; shift 1;;
    --maxremove=*) maxremove="${1#*=}";  shift 1;;
    --doremove)    doremove=1;           shift 1;;
    --interactive) rminteractive="-i";   shift 1;;
    --verbose)     verbose=1;            shift 1;;
    --help) usage; exit 0;;
    --dirname|--cachesize|--factor|--maxremove)
	echo "$1 requires an argument" >&2;
	exit 1;;
    
    -*) echo "unknown option: $1" >&2;  exit 1;;
    *) echo "unknown argument: $1" >&2; exit 1;;
  esac
done

# checking mandatory arguments
if [[ $dirname == "" || $cachesize == "" ]] 
then
    echo "mandatory arguments not passed"
    usage
    exit 1
fi
# cachesize is in kilo byte
cachesize=$(echo "($cachesize*1000)/1" | bc 2> /dev/null)
if [[ $cachesize -le  0 ]] 
then
    echo "cachesize invalid ($cachesize), should be > 0"
    usage
    exit 1
fi

current_cachesize=$(du -s --apparent-size $dirname 2> /dev/null | cut -f1)
current_freespace=$(( $cachesize - $current_cachesize ))

wanted_freespace=0
if [[ $current_freespace -le 0 ]];
then
    wanted_freespace=$(( $current_freespace * -1 ))
fi
wanted_freespace=$( echo "$wanted_freespace + ($cachesize*$freefactor)/1" | bc 2> /dev/null)

   
if [[ $verbose == 1 ]]
then
    echo "Max cachesize set  (KB)  = $cachesize"
    echo "Current cachesize  (KB)  = $current_cachesize"
    echo "Current free space (KB)  = $current_freespace"
    echo "Free space needed  (KB)  = $wanted_freespace"
    echo
    echo
fi


if (( $current_freespace > $wanted_freespace ))
then
    if [[ $verbose == 1 ]]
    then
	echo "There is enough free space, nothing to do!"
    fi
    exit 0
else
    if [[ $verbose == 1 ]]
    then
	echo "Try to regain $(( $wanted_freespace-$current_freespace )) KB of space ..."
    fi
fi


sum=0
while IFS= read -r line; do
    size=$( echo $line | cut -d' ' -f 2)
    sum=$(( $sum + $size ))
    array+=("$line" $sum)
done < <( find $dirname -type f -printf '%A@ %s %p\n' 2> /dev/null | sort -k 1 | head -n $maxremove )


if [[ $doremove == 0 ]];
then
    if [[ $verbose == 1 ]]
    then
	# just print all files
	for((i=0;i<${#array[@]};i+=2)); do
	    echo ${array[i]}
	done
    fi
else
    #now wanted_freespace is in byte
    wanted_freespace=$(( wanted_freespace*1000 ))

    sum=0
    for((i=0;i<${#array[@]};i+=2)); do
	# remove the oldest one and then check if it is enough
	filename=$( echo ${array[i]} | cut -d' ' -f 3 )
	$( rm $rminteractive $filename )
	if (( ${array[i+1]} >= $wanted_freespace ))
	then
	    break
	fi
    done
fi
exit 0

