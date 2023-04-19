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
        print("[**] New server socket thread started for spark connetion from " + ip + ":" + str(port)) 
 
    def run(self): 
        #for i in range (10) : 
        #    MESSAGE = "File DATA " + str(i) + "\n"
        #    self.conn.sendall(MESSAGE.encode())  # echo 
        
        while True:
            #print("[**] Get from queue for "+ self.ip + ":" + str(self.port))
            #print(self.q)
            #print("[**] queue size " + self.q.qsize())
            data_file = self.q.get()
           
            try:
                #print("[**] " + data_file)
                if data_file is None:
                    end_msg = {'project_id':'EOS','file_name': '','file_type': '','data':''}
                    self.conn.send(json.dumps(end_msg).encode() + b'\n')
                    #self.conn.send(json.dumps('{"project_id":"EOS","file_name":"","file_type": "","data":""}').encode() + b'\n')
                    print("[**] Data is None... EOS")
                    #self.q.task_done()
                    #return
                    #self.conn.close()
                    break
                time.sleep(_CONFIG["send_sleep_debug"])
                msg = json.dumps(data_file)
                print("msg: " + data_file["file_name"]);
                self.conn.send(msg.encode() + b'\n')
            except:
                print("[**] Unexpected socket error - Spark worker has close socket")
                self.conn.close()
                return
        
            #print("[**] Project:" + data_file['project_id'] +  "File:"+ data_file['file_name'] + " sended to "+ self.ip + ":" + str(self.port) )
            #logging.debug("[**] File "+ data_file['file_name'] +" sended to ")
            #self.q.task_done()        
            #if data_file == "EOS":
            #    break

        while True:
            try:
                #end_msg = {'project_id':'EOS','file_name': '','file_type': '','data':''}
                #self.conn.send(json.dumps(end_msg).encode() + b'\n')
                #self.conn.send("Test connection")
                   
                result = self.conn.getsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE)
                if result == 0:
                    print('Il socket è stato chiuso dal client')
                    time.sleep(10)
                    self.conn.close()
                    break
                else:
                    print('Il socket è ancora aperto')
                #print("[**] Data is None... EOS")
                    
            except:
                print("[**] Spark worker has close socket")
                self.conn.close()
                return

        return
