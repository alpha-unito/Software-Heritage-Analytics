import json
import logging
import os
from pathlib import Path
from multiprocessing import Process, Queue
import socket 
#from threading import Thread 
import time
from config import _CONFIG

#logging.basicConfig(level=logging.DEBUG,format='[%(levelname)s] (%(threadName)-9s) %(message)s',)

class SparkRequest(): 
 
    def __init__(self,conn,ip,port,q): 
        #Thread.__init__(self) 
        self.ip = ip 
        self.port = port 
        self.conn = conn
        self.q = q
        #print(self.q)
        #print(self.q.qsize())
        print("[**] New server socket thread started for " + ip + ":" + str(port)) 
 
    def run(self): 
        #for i in range (10) : 
        #    MESSAGE = "File DATA " + str(i) + "\n"
        #    self.conn.sendall(MESSAGE.encode())  # echo 
        
        while True:
            #print("[**] Get from queue")
            #print(self.q)
            #print(self.q.qsize())
            data_file = self.q.get()
            #print(data_file)
            if data_file is None:
                self.conn.send(json.dumps("{'project_id':'EOS','file_name': '','file_type': '','data':''}").encode() + b'\n')
                print("[**] Data is None... EOS")
                #self.q.task_done()
                return
            time.sleep(_CONFIG["send_sleep_debug"])
            self.conn.send(json.dumps(data_file).encode() + b'\n')
            #print("[**] File "+ data_file['file_name'] +" sended to ")
            #logging.debug("[**] File "+ data_file['file_name'] +" sended to ")
            #self.q.task_done()        
            #if data_file == "EOS":
            #    break
        self.conn.close()
