import sys
import argparse
import socket 
import json
import os
import subprocess
import time
from datetime import datetime
from config import _CONFIG


def str2bool(v):
    if isinstance(v, bool):
        return v
    if v.lower() in ('yes', 'true', 't', 'y', '1'):
        return True
    elif v.lower() in ('no', 'false', 'f', 'n', '0'):
        return False
    else:
        raise argparse.ArgumentTypeError('Boolean value expected.')


parser = argparse.ArgumentParser(description="Test Client pycapio")
parser.add_argument("-a", type=str, help="Server IP address")
parser.add_argument("-p", type=int, help="Server Port")
parser.add_argument("-r", type=str, help="Recipe json file")
parser.add_argument("-n", type=str, help="Set App name")
parser.add_argument("-d", type=str2bool, nargs='?',const=True, default=False,help="Activate spark dry run mode.")
parser.add_argument("-m", type=str, default="127.0.0.1", help="Set spark master address")
parser.add_argument("-e", type=str, default="20G", help="Set spark memory")
#parser.add_argument("-s", type=str, default="127.0.0.1", help="Set stream address")
args = parser.parse_args()



profile_time_start = datetime.now()

host = args.a
port = args.p
recipe_file = args.r
app_name = args.n
spark_master_addr = args.m
stream_addr = args.a
dry_run = args.d
spark_mem = args.e
spark_submit_path="~/spark-3.3.1-bin-hadoop3-scala2.13/bin/"

print("dry_run:" + str(dry_run) )

start_time = time.perf_counter()
print("Receipe file:" + recipe_file) 
recipe_json = json.load(open(recipe_file))
if "app_name" not in recipe_json.keys():
    recipe_json["app_name"] = recipe_file.split("/")[-1]
    print("App name not present set to " +recipe_json["app_name"])


default_path = 'app/'
if app_name is not None:
    print("Change App name: " +recipe_json["app_name"]+ " --> " + app_name)
    recipe_json["app_name"] = app_name
streams_num = recipe_json["rules"]["num_slave"]
spark_app_name = recipe_json["app"]
if "app_path" not in recipe_json.keys():
    spark_app_path = default_path
else:
    spark_app_path = recipe_json["app_path"]
print("App path:"+spark_app_path)
dstream_time = recipe_json["rules"]["dstream_time"]
BUFFER_SIZE = 10000 
recipe = json.dumps(recipe_json)
print(recipe)
tcpClientA = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
tcpClientA.connect((host, port))

#MESSAGE = input("Test Client: Enter message / Enter exit:") 
tcpClientA.sendall(bytes(recipe,encoding="utf-8"))     


addr_local_master = ' local['+ str({streams_num*2}) +'] '
addr_master= "spark://" + str(spark_master_addr) + ":7077 "
data = tcpClientA.recv(BUFFER_SIZE)
print ("Received data:", data.decode())
if data.decode() != "Abort":
    spark_port = data.decode()
    print("Port:" + spark_port)
    #print("Launch spark APP") 
    
    # --conf spark.yarn.submit.waitAppCompletion=false
    # default class = "SHAmain"
    spark_conf ='--conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:///var/log/custom-log4j.properties" --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:///var/log/custom-log4j.properties"'
    param_spark = f'{spark_conf} --class "{recipe_json["app_name"]}" --master {addr_master} --name {recipe_json["app_name"]} --deploy-mode client  --executor-memory {spark_mem} --total-executor-cores {streams_num*4}' 
    params_app = str(streams_num) + " " + stream_addr + " " + spark_port + " " + str(dstream_time) 
    cmd = spark_submit_path + 'spark-submit ' + param_spark  + ' \
           ' + spark_app_path + spark_app_name +' '+ params_app +' '
    print(cmd)
    #os.system(cmd)
    if not dry_run:
        print("Launch spark APP")
        #time.sleep(1)
        subprocess.run(cmd,shell=True)
    else:
        print("Spark dry run")

data = tcpClientA.recv(BUFFER_SIZE)
print ("Client received data:", data.decode())
execution_time = time.perf_counter() - start_time
print(f"APP execution time: {execution_time:0.4f} seconds ")
profile = open('profile.txt', 'a')
profile.write(f'[{profile_time_start}] {args} ({execution_time:0.4f} seconds)\n')
profile.close()
#while MESSAGE != 'exit':
#    tcpClientA.send(MESSAGE)     
#    data = tcpClientA.recv(BUFFER_SIZE)
#    print ("Client received data:", data)
#    MESSAGE = input("tcpClientA: Enter message to continue/ Enter exit:")i



tcpClientA.close() 
