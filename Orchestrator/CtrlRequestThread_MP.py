import json
import logging
import os
import sys
from pathlib import Path
import requests
#from Cache import cache
import socket 
from threading import Thread 
import threading
from SparkRequest_MP import SparkRequest
from CacheManagerPool import CacheManager
from multiprocessing import Process, Queue
import io
import requests
import base64
import time
from utils import *
from config import _CONFIG
#import tau
#import pytau

#logging.basicConfig(level=logging.DEBUG,format='[%(levelname)s] (%(threadName)-9s) %(message)s')
#logging.basicConfig(format="%(module)s : %(funcName)s : (Process Details : (%(process)d, %(processName)s), Thread Details : (%(thread)d, %(threadName)s))\nLog Message : %(message)s\n",
#                    datefmt="%d-%B,%Y %I:%M:%S %p",
#                    level=logging.INFO)


#class CtrlRequestThread(Thread): 
class CtrlRequestThread():
    def __init__(self,conn,ip,port,num_client): 
        #Thread.__init__(self) 
        self.ip = ip 
        self.client_num = num_client
        self.port = port 
        self.conn = conn
        self.queue_list = []
        print("[+] New CTRL server socket thread started for " + ip + ":" + str(port))
        
        # CONFIG
        self.SPARKCLINET_TCP_IP = _CONFIG["bind_address"]
        self.SPARKCLINET_TCP_PORT = _CONFIG["spark_client_base_port"]
        self.SPARKCLINET_BUFFER_SIZE = _CONFIG["buffer_size"]
        self.requestThread = [] 
        self.recipe = None
        self.CacheMng = None
        self.dry_run = True
        if self.dry_run:
            print("DRY RUN enable");

        #self.logging = self.setup_logger('CTRL',f"logs/APP-{self.recipe['app_name']}.log")
 #       self.tau_profile = pytau.profileTimer("ctrl_app_req")
 
    def run(self): 
  #      pytau.start(self.tau_profile)
        pid = os.getpid()
        print(f"[+] New CTRL process pid: {os.getpid()}")
        stringa = self.conn.recv(_CONFIG["buffer_size"]).decode("utf-8")
        print(stringa)
        self.recipe = json.loads(stringa)

        self.logging = logger = Logger(self.recipe["app_name"])
        self.logging.debug("[+] Recipe received from Dashboard: ", self.recipe)
        print("Num request todo:" + str(len(self.recipe["projects"])))
        tcpCTRLServer = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tcpCTRLServer.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        tcpCTRLServer.bind((self.SPARKCLINET_TCP_IP, self.SPARKCLINET_TCP_PORT + self.client_num)) 
        
        
        #init spark stream thread queue
        print(f'[+] Create {self.recipe["rules"]["num_slave"]} streaming queue')
        for i in range(self.recipe["rules"]["num_slave"]):
            self.queue_list.append(Queue())
            
        # run thread Cache Manager  
        print("[+] Run APP "+self.recipe["app_name"])
        self.CacheMng = CacheManager(self.recipe["app_name"],self.queue_list,self.recipe["projects"],self.recipe["rules"]["num_slave"],self.dry_run)
        CacheMng_p = Process(target=self.CacheMng.run)
        CacheMng_p.start()
        #self.CacheMng.start()
        #self.requestThread.append(self.CacheMng) 


        # send ready message to dashboard with the port numeber
        MESSAGE = str(self.SPARKCLINET_TCP_PORT + self.client_num)
        self.conn.sendall(MESSAGE.encode())  # echo 
        
        if not self.dry_run:
            # accept connection from spark
            print(f"[+ {self.recipe['app_name']}] CTRL thread waiting for spark connections..")
            for q in self.queue_list:
            
                tcpCTRLServer.listen(4) 
                self.logging.debug(f"[+ {self.recipe['app_name']}] CTRL server waiting for connections " + self.SPARKCLINET_TCP_IP + ":" + str(self.SPARKCLINET_TCP_PORT) + " from SPARK clients...") 
                (req_conn, (req_ip,req_port)) = tcpCTRLServer.accept() 
                self.logging.debug(f"[+ {self.recipe['app_name']}] New spark worker connection from: " + str(req_ip) )
                print(f"[+ {self.recipe['app_name']}] New spark worker connection from: " + str(req_ip) )
                newreq = SparkRequest(req_conn,req_ip,req_port,q) 
                newreq_p = Process(target=newreq.run)
                newreq_p.start() 
                self.requestThread.append(newreq_p) 

    
        # wait for end Cache Manager
        print(f"[+ {self.recipe['app_name']}] wait Cache Manager end...")
        #self.CacheMng.join()
        CacheMng_p.join()
        print(f"[+ {self.recipe['app_name']}] Cache Manager end")


        if not self.dry_run:
            # wait for stream thread ending
            print(f"[+ {self.recipe['app_name']}] Cache Manager end")
            for t in self.requestThread: 
                print(f"[+ {self.recipe['app_name']}] wait streaming queue end...")
                t.join()
        
        # send end message to dashboard  i
        print(f"[+ {self.recipe['app_name']}]  SEND Application ended " +self.recipe["app_name"] )
        MESSAGE = "Application ended"
        self.conn.sendall(MESSAGE.encode())  # echo 

        self.conn.close()
        #os._exit(os.EX_OK)
        
        sys.exit()
   #     pytau.stop(self.tau_profile)
   #     pytau.dbDump()
        #cache.info()
        
   
            
   
