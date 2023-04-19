import json
import logging
import os
import sys
import socket 
from pathlib import Path
from multiprocessing import Process
from threading import Thread 
#from CtrlRequestThread import CtrlRequestThread
from CtrlRequestThread_MP import CtrlRequestThread
from types import SimpleNamespace
#from Cache import cache
from config import _CONFIG
import signal 

# CONFIG
CTRL_TCP_IP = _CONFIG["bind_address"] 
CTRL_TCP_PORT = _CONFIG["port"] 
CTRL_BUFFER_SIZE = 1024
MAX_CLIENT =  _CONFIG["num_max_app_client"]
#cache.num_element = 10

tcpServer = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
tcpServer.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) 
tcpServer.bind((CTRL_TCP_IP, CTRL_TCP_PORT)) 
ctrlThreads = [] 

#print(cache) 

#def handler(signum, frame):
#    res = input("Ctrl-c was pressed. Do you really want to exit? y/n ")
#    if res == 'y':
#        exit(1)
 
#signal.signal(signal.SIGINT, handler)

def exit_gracefully(signum, frame):
    # restore the original signal handler as otherwise evil things will happen
    # in raw_input when CTRL+C is pressed, and our signal handler is not re-entrant
    signal.signal(signal.SIGINT, original_sigint)

    try:
        if input("\nReally quit? (y/n)> ").lower().startswith('y'):
            sys.exit(1)

    except KeyboardInterrupt:
        print("Ok ok, quitting")
        sys.exit(1)

    # restore the exit gracefully handler here    
    signal.signal(signal.SIGINT, exit_gracefully)

original_sigint = signal.getsignal(signal.SIGINT)
signal.signal(signal.SIGINT, exit_gracefully)


num_client = 0
ctrlClient = []
while True: 
    try:
        tcpServer.listen(4) 
        print("PyCachemire server : Waiting for connections from Dashboard clients on port "+ str(CTRL_TCP_PORT) +" ...") 
        (conn, (ip,port)) = tcpServer.accept() 
        ctrl = CtrlRequestThread(conn,ip,port,num_client) 
        newclient = Process(target=ctrl.run)
        newclient.start() 
        ctrlClient.append(newclient) 
        num_client = num_client + 1    
    except Exception as e: print(e)


print("PyCachemire wait client end..")
for t in ctrlClient: 
    t.join()

print("PyCachemire exit")

sys.exit(0)
