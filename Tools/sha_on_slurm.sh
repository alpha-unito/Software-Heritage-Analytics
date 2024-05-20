#!/bin/bash
#
# get environment variables
CACHEMIRE_DATA_DIR=~/Software-Heritage-Analytics/Example/CACHE/
GLOBAL_RANK=$SLURM_PROCID
CPUS=`grep -c ^processor /proc/cpuinfo`
MEM=$((`grep MemTotal /proc/meminfo | awk '{print $2}'`/1000)) # seems to be in MB
MASTER_ADDR=$(scontrol show hostnames "$SLURM_JOB_NODELIST" | head -n 1)
LOCAL_IP=$(hostname -I | awk '{print $2}')
NODE_HOST=$(hostname)

spack load sha
spack load scancode-toolkit

echo "NODE: $LOCAL_IP"

spack load openjdk

# setup the master node
if [ $GLOBAL_RANK == 0 ]
then
    # print out some info
    echo -e "MASTER ADDR: $MASTER_ADDR\tGLOBAL RANK: $GLOBAL_RANK\tCPUS PER TASK: $CPUS\tMEM PER NODE: $MEM"

    # then start the spark master node in the background
    $SPARK_HOME/sbin/start-master.sh -p 7077 -h $LOCAL_IP

    echo "Run cachemire..."
    nohup cachemire -d $CACHEMIRE_DATA_DIR > ca.log &
    echo "Run orchestrator"
    nohup orchestrator > or.log &
    mkdir logs
    echo $MASTER_ADDR:4320
   
fi

sleep 5


MEM_IN_GB=$(($MEM / 1000))
MEM_IN_GB="$MEM_IN_GB"G
echo "MEM IN GB: $MEM_IN_GB"
echo $(hostname)
export SPARK_NO_DAEMONIZE=1
mkdir -p $SPARK_HOME/logs/$LOCAL_IP
export SPARK_LOG_DIR=$SPARK_HOME/logs/$LOCAL_IP
$SPARK_HOME/sbin/start-worker.sh -c $CPUS -m $MEM_IN_GB "spark://$MASTER_ADDR:7077"  
echo "Hello from worker $GLOBAL_RANK"

Loop infinito per mantenere lo script in esecuzione
while true; do
   sleep 60
done