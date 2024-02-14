import sys
import argparse
import socket
import json
import os
import subprocess
import time
import uuid
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
parser.add_argument("-d", type=str2bool, nargs='?', const=True,
                    default=False, help="Activate spark dry run mode.")
parser.add_argument("-m", type=str, default="127.0.0.1",
                    help="Set spark master address")
parser.add_argument("-e", type=str, default="20G", help="Set spark memory")
parser.add_argument("-D", type=str2bool, nargs='?', const=True,
                    default=False, help="Activate spark deploy-mode client (debug mode).")
# parser.add_argument("-s", type=str, default="127.0.0.1", help="Set stream address")
parser.add_argument("-dir", type=str, help="Dir to save json app output")
parser.add_argument("-sb", type=str, help="Scancode exec path")
parser.add_argument("-si", type=str, help="Scandode index")
parser.add_argument("-rp", type=str, help="Ramdisk path")
parser.add_argument("-gp", type=str, help="Graph path")
parser.add_argument("-op", type=str, help="Output path")

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
output_dir = args.dir
scancode_exe_path = args.sb
scancode_index_file = args.si
ramdisk_path = args.rp
graph_path = args.gp
output_path = args.op

if args.D is False:
    spark_deploy_mode = "cluster"
else:
    spark_deploy_mode = "client"
spark_submit_path = "~/spark-3.3.2-bin-hadoop3-scala2.13/bin/"

print("dry_run:" + str(dry_run))

start_time = time.perf_counter()
print("Receipe file:" + recipe_file)
recipe_json = json.load(open(recipe_file))
if "app_name" not in recipe_json.keys():
    recipe_json["app_name"] = recipe_file.split("/")[-1]
    print("App name not present set to " + recipe_json["app_name"])


run_app_path = '~/Software-Heritage-Analytics/Orchestrator/app/'
default_path = 'app/'
workers_host = ["node-1", "node-2", "node-3", "node-4"]


if app_name is not None:
    print("Change App name: " + recipe_json["app_name"] + " --> " + app_name)
    recipe_json["app_name"] = app_name

streams_num = recipe_json["rules"]["num_slave"]

spark_app_name = recipe_json["app"]

if "app_path" not in recipe_json.keys():
    spark_app_path = run_app_path
else:
    spark_app_path = recipe_json["app_path"]

print("App path:"+spark_app_path)
dstream_time = recipe_json["rules"]["dstream_time"]
BUFFER_SIZE = 10000


# deploy app jar on spark worker node
print("deploy app jar on saprk worker node")
for whost in workers_host:

    source_file = default_path+spark_app_name
    destination = f'{whost}:{run_app_path}'

    scp_command = ['scp', source_file, destination]

    try:
        # Run the scp command
        subprocess.run(scp_command, check=True)
        print(f'File copied successfully from {source_file} to {destination}')
    except subprocess.CalledProcessError as e:
        print(f'Error copying file: {e}')


print("Send recipe to Orchestrator...")
recipe = json.dumps(recipe_json)
# print(recipe)
tcpClientA = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
tcpClientA.connect((host, port))

# MESSAGE = input("Test Client: Enter message / Enter exit:")
tcpClientA.sendall(bytes(recipe, encoding="utf-8"))


addr_local_master = ' local[' + str({streams_num*2}) + '] '
addr_master = "spark://" + str(spark_master_addr) + ":7077 "
data = tcpClientA.recv(BUFFER_SIZE)
print("Received data from Orchestrator:")
if data.decode() != "Abort":
    spark_port = data.decode()
    print("Port:" + spark_port)
    # print("Launch spark APP")

    # --conf spark.yarn.submit.waitAppCompletion=false
    # default class = "SHAmain"
    instance = streams_num
    spark_conf = f'--conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:///beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/custom-log4j.properties" \
                 --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:///beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/custom-log4j.properties" \
                 --conf "spark.executor.instances={instance}" \
                 --conf "spark.executor.memory=110g" \
                 --conf "spark.executor.cores=36" \
                 --conf "spark.default.parallelism={instance * 36}" \
                 --conf "spark.app.id={app_name}_{uuid.uuid4()}"'
    # spark_conf ='--conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:///var/log/custom-log4j.properties" \
    #             --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:///var/log/custom-log4j.properties" \
    #             --conf spark.worker.logDirectory=/tmp'
    # --executor-cores 8 --num-executors 4' # --num-executors {streams_num}'
    # --executor-memory {spark_mem}'
    param_spark = f'{spark_conf} --class "{recipe_json["app_name"]}" --master {addr_master} --name {recipe_json["app_name"]} --deploy-mode {spark_deploy_mode}'
    params_app = str(streams_num) + " " + stream_addr + \
        " " + spark_port + " " + str(dstream_time)
    cmd = spark_submit_path + 'spark-submit ' + param_spark + ' \
           ' + spark_app_path + spark_app_name + ' ' + params_app + ' ' + output_dir + ' ' + scancode_exe_path + ' ' + scancode_index_file + ' ' + ramdisk_path + ' ' + graph_path + ' ' + output_path + ' '
    print(cmd)
    # os.system(cmd)
    if not dry_run:
        print("Launch spark APP")
        # time.sleep(1)
        subprocess.run(cmd, shell=True)
        # TODO: prova di lancio non bloccante, con estrazione dell'id dell'esecuzione
#            "--conf", "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:///tmp/custom-log4j.properties",
#            "--conf", "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:///tmp/custom-log4j.properties",
       # spark_submit_command = [
       #     os.path.expanduser("~/spark-3.3.1-bin-hadoop3-scala2.13/bin/spark-submit"),
       #     "--class", "licensectrl",
       #     "--master", "spark://node-1:7077",
       #     "--name", "licensectrl",
       #     "--deploy-mode", "cluster",
       #     "--executor-memory", "20G",
       #     "--total-executor-cores", "16",
       #     "/tmp/licensectrl.jar", "4", "172.20.85.122", "4325", "2"
       # ]
#        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
#        stdout, stderr = process.communicate()
#        process.wait()
#        print(process.returncode)
#        if process.returncode != 0:
#            print("Submit failed!")
#            print(stderr.decode("utf-8"))
#            quit()
#        else:
#            print("Submit ok!")
#            application_id = None
#            import re
#            while not application_id:
#                spark_output = stdout.decode("utf-8")
#                spark_err = stderr.decode("utf-8")
#                print(spark_output, spark_err)
#                match = re.search(f"application_\d+_\d+", spark_output)
#                if match:
#                    application = match.group()
#                    print(f"Application ID: {application_id}")
#                else:
#                    print("Unable to extract the application ID.")
#                    time.sleep(1)
#
    else:
        print("Spark dry run")


# wait for end of application
data = tcpClientA.recv(BUFFER_SIZE)
print("Client received data:", data.decode())
execution_time = time.perf_counter() - start_time
print(f"APP execution time: {execution_time:0.4f} seconds ")
profile = open('profile.txt', 'a')
profile.write(
    f'[{profile_time_start}] {args} ({execution_time:0.4f} seconds)\n')
profile.close()
# while MESSAGE != 'exit':
#    tcpClientA.send(MESSAGE)
#    data = tcpClientA.recv(BUFFER_SIZE)
#    print ("Client received data:", data)
#    MESSAGE = input("tcpClientA: Enter message to continue/ Enter exit:")i


tcpClientA.close()
