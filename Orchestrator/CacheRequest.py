import json
#import os
#import sys
#from stat import *
import gzip
import io
import tarfile
import requests
import base64
import time
#import queue
#from threading import Thread
from multiprocessing import Process, Queue
#from SWHVault import SWHVault
from pprint import pprint
from config import _CONFIG
import socket
import struct
import logging
from utils import *
import sys
import datetime

#logging.basicConfig(level=logging.DEBUG,format='[%(levelname)s] (%(threadName)-9s) %(message)s')
#logging.basicConfig(format="[%(module)s->%(funcName)s : P(%(processName)s) T(%(threadName)s)] %(message)s",
#                    datefmt="%d-%B,%Y %I:%M:%S %p",
#                    level=logging.DEBUG)

OP_SIZE    =     1
DATA_SIZE  =     8
KEY_SIZE   =     40
OP_GET     =     0
OP_PUT     =     1
OP_OK      =     20
OP_FAILED  =     21

#  https://eli.thegreenplace.net/2011/05/18/code-sample-socket-client-thread-in-python
class CacheRequest():
    def __init__(self, app_name, project_id, project_lang):
    #def __init__(self, name , daemon , q, stop_event): 
    #Process.__init__(self,name=name, daemon=daemon, args=(q, stop_event))     
        self.socket = None
        self.app_name = app_name
        #self.queue_list = queue_list
        self.project_id = project_id
        self.project_lang = project_lang
        self.file_list = None
        #self._handle_CONNECT(_CONFIG["default_cache_ip"] , _CONFIG["default_cache_port"]) 
        self.vault_type =  _CONFIG["swh_vault_type"]
        self.prefix =  _CONFIG["swh_prefix"]
        self.swh_api_endpoint = _CONFIG["swh_api_endpoint"]
        self.polling_time = _CONFIG["swh_polling_time"]
        self.cache_error = ""

        self.logging = Logger(app_name)
        #self.logging = self.setup_logger('CACHE',f"logs/APP-{self.app_name}.log")

    def __del__(self):
        return

    def setup_logger(self,name, log_file, level=logging.DEBUG):
        """To setup as many loggers as you want"""
        if log_file == "":
            log_file = "logs/test"
        #formatter = logging.Formatter("[%(module)s->%(funcName)s : P(%(processName)s) T(%(threadName)s)] %(message)s")
        #handler = logging.FileHandler(log_file)
        #handler.setFormatter(formatter)

        #logger = logging.getLogger(name)
        #logger.setLevel(level)
        #logger.addHandler(handler)

        #return logger

    def run(self):
        #self.app_name = app_name
        #self.queue_list = queue_list
        #self.project_id = project_id
        #self.project_lang = project_lang
        
        
        # log_info format ['call_time', 'op', 'project_id', 'outcome', 'execution_time', 'error' ]
        log_info = dict()
        log_info["project_id"] = self.project_id
        log_info["call_time"] = datetime.datetime.now().timestamp()
        log_info["op"] = "Connect"

        ret = self._handle_CONNECT(_CONFIG["default_cache_ip"] , _CONFIG["default_cache_port"])
        if ret is False:
            log_info["outcome"] = "fail"
            log_info["error"] = f"{_CONFIG['default_cache_ip']} connaction fail"
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)

            #self.logging.debug("ERROR cache connection fail:")

            log_info["call_time"] = datetime.datetime.now().timestamp()
            log_info["op"] = "SWHvault"
            
            ret = self.SWHVault()
            if ret is False:
                log_info["outcome"] = "fail"
                log_info["error"] = "fail"
                log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
                logCSV(self.app_name,log_info)
         
                #self.logging.debug("ERROR project_id key:" + self.project_id)
                return False

            log_info["outcome"] = "ok"
            log_info["error"] = ""
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)
            return self.file_list


        
        log_info["call_time"] = datetime.datetime.now().timestamp()
        log_info["op"] = "GET"
        p_id = self.project_id.encode()
        op = (OP_GET).to_bytes(OP_SIZE,byteorder='big')
        msg = struct.pack(f'c{KEY_SIZE}s', op, p_id)
        self.logging.debug("SEND GET:" + str(msg))
        
        # send GET to cache
        try:
            self.socket.sendall(msg)
        except IOError as e:
            logging.debug("Can't send GET project_id:" +self.project_id + " " +  str(e))   
            
            log_info["outcome"] = "fail"
            log_info["error"] = "fail"
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)

            return False

        # verify GET return
        tar_file_byte = self._handle_RECEIVE()
        if tar_file_byte is False:
            self.logging.debug("Cache MISS "+self.project_id)
            log_info["outcome"] = "MISS"
            log_info["error"] = self.cache_error
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)
            

            # CACHE MISS retrieve for SWH
            tar_fileobj = self.retrieve_from_SWH()
            if tar_fileobj is False:
                #self.logging.debug("ERROR project_id key:" + self.project_id)
                return False 

            self.put(tar_fileobj)
            self.socket.close()
            # TODO to optimize.... return file list before put to chache 
            return self.file_list

        else:
            self.logging.debug("Cache HIT "+self.project_id)
            self.socket.close()
            log_info["outcome"] = "HIT"
            log_info["error"] = ""
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)


            # schedule stream file
            inmemory_tar_file = io.BytesIO(tar_file_byte)

            self.file_list = unpackProjectFile(self.project_id,inmemory_tar_file)

            #scheduleFileOnStream(self.project_lang,self.queue_list,file_list)

        return self.file_list

    def retrieve_from_SWH(self):
        log_info = dict()
        log_info["project_id"] = self.project_id
        log_info["call_time"] = datetime.datetime.now().timestamp()
        log_info["op"] = "SWHvault"
        ret = self.SWHVault()
        if ret is False:
            log_info["outcome"] = "fail"
            log_info["error"] = "fail"
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)
            #self.logging.debug("ERROR project_id key:" + self.project_id)
            return False

        log_info["outcome"] = "ok"
        log_info["error"] = ""
        log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
        logCSV(self.app_name,log_info)
        return ret

    def put(self,tar_file):
        log_info = dict()
        log_info["project_id"] = self.project_id
        log_info["call_time"] = datetime.datetime.now().timestamp()
        log_info["op"] = "PUT"
        p_id = self.project_id.encode()
        op = (OP_PUT).to_bytes(OP_SIZE,byteorder='big')
        file_byte = tar_file.getvalue()
        size = (len(file_byte)).to_bytes(DATA_SIZE,byteorder='big')
        msg = struct.pack(f'c{KEY_SIZE}s{DATA_SIZE}s', op, p_id, size)
        self.logging.debug("SEND PUT:" + str(msg))
        #logging.debug("SEND PUT  msg size:" + str(len(msg)) + " file size:"+ str(len(file_byte)) )
        
        # send PUT header to cache
        try:
            self.socket.sendall(msg)
        except IOError as e:
            #self.logging.debug("Can't send PUT header project_id:" +self.project_id + " " +  str(e))
            log_info["outcome"] = "fail"
            log_info["error"] = "fail send header"
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)
            return False

        # send PUT data to cache
        try:
            #self.logging.debug("SEND PUT send data")
            self.socket.sendall(file_byte)
        except IOError as e:
            #self.logging.debug("Can't send PUT data project_id:" +self.project_id + " " +  str(e))
            log_info["outcome"] = "fail"
            log_info["error"] = "fail send file"
            log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
            logCSV(self.app_name,log_info)
            return False

        # receive 
        RPL_HDR_SIZE = OP_SIZE+DATA_SIZE
        header_data = self._recv_n_bytes(RPL_HDR_SIZE)
        #self.logging.debug("receive PUT " + str(header_data))
        if len(header_data) == RPL_HDR_SIZE:
            op, data = struct.unpack(f'b{DATA_SIZE}s', header_data)
            #logging.debug("receive PUT op:" + str(op) + " data:" + str(data.decode()))
            if op == OP_FAILED:
                msg_len = int.from_bytes(data, byteorder='big')
                data = self._recv_n_bytes(msg_len)
                log_info["outcome"] = "fail"
                log_info["error"] = "fail receive"
                log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
                logCSV(self.app_name,log_info)

                if len(data) == msg_len:
                    self.logging.debug('PUT error:'+ data )
                    return False
                else:
                    self.logging.debug('file receive lenght error')
                    return False
            if op == OP_OK:
                log_info["outcome"] = "ok"
                log_info["error"] = ""
                log_info["execution_time"] = datetime.datetime.now().timestamp() - log_info["call_time"]
                logCSV(self.app_name,log_info)
                return True 

        #self.logging.debug('Header corrupted')
        return False
    
    def get_file_from_socket(data_sock):
        message_size = int.from_bytes(socket_read(data_sock, 4), byteorder="little")
        data = np.frombuffer(socket_read(data_sock, message_size), dtype=np.uint8)
        return io.BytesIO(data)

    def socket_read(sock, expected):
        #Read expected number of bytes from sock Will repeatedly call recv until all expected data is received
        buffer = b''
        while len(buffer) < expected:
            buffer += sock.recv(expected - len(buffer))
        return buffer

    def _handle_CONNECT(self, ip, port):
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((ip, port))
            
            #self.logging.debug("cache connected")
            return True
        except IOError as e:
            logging.debug(str(e))
            return False

    def _handle_RECEIVE(self):
        try:
            RPL_HDR_SIZE = OP_SIZE+DATA_SIZE
            header_data = self._recv_n_bytes(RPL_HDR_SIZE)
            #logging.debug("receive GET " + str(header_data))
            if len(header_data) == RPL_HDR_SIZE:
                op, data = struct.unpack(f'b{DATA_SIZE}s', header_data)
                #logging.debug("receive GET op:" + str(op))
                
                if op == OP_OK:
                    msg_len = int.from_bytes(data, byteorder='big')
                    #logging.debug("receive GET data size:" + str(msg_len))
                    data = self._recv_n_bytes(msg_len)
                    if len(data) == msg_len:
                        
                        return data
                    else:
                        #self.logging.debug('file receive lenght error')
                        return False
                if op == OP_FAILED:

                    msg_len = int.from_bytes(data, byteorder='big')
                    #logging.debug("receive GET data size:" + str(msg_len))
                    data = self._recv_n_bytes(msg_len)
                    if len(data) == msg_len:
                        self.cache_error = data.decode()
                        #self.logging.debug("receive GET error:" + data.decode())
                    else:
                        return False
                        #self.logging.debug('file receive lenght error')    
                    return False

            #self.logging.debug('Socket closed prematurely')
        except IOError as e:
            #self.logging.debug("IOError " + str(e))
            return False

    def _recv_n_bytes(self, n):
        """ Convenience method for receiving exactly n bytes from
            self.socket (assuming it's open and connected).
        """
        data = b''
        while len(data) < n:
            chunk = self.socket.recv(n - len(data))
            #logging.debug("chunk:"+str(chunk))
            if chunk == b'':
                break
            data += chunk
        return data

    def SWHVault(self):
        try:
            #self.logging.debug("POST -> " + f"{self.swh_api_endpoint}/{self.vault_type}/{self.prefix}{self.project_id}/")
            response = requests.post(f"{self.swh_api_endpoint}/{self.vault_type}/{self.prefix}{self.project_id}/")
        except requests.exceptions.RequestException as e:
            #self.logging.debug("POST ERROR " + str(e))
            return False
       
        if response.status_code != 200:
            #self.logging.debug("POST ERROR ret code " + str(response.status_code))
            return False

        data = response.json()
        #logging.debug(data)
        tar_file_data = None
        while True:
            if data["status"] == "done":
                tar_file_data = requests.get(f"{data['fetch_url']}")
                break
            time.sleep(self.polling_time)
            self.logging.debug("Try again... GET -> " + f"{self.swh_api_endpoint}/{self.vault_type}/{self.prefix}{self.project_id}/")
            response = requests.get(f"{self.swh_api_endpoint}/{self.vault_type}/{self.prefix}{self.project_id}/")
            data = response.json()
            #logging.debug(data)

        self.logging.debug("DONE!" + f"{self.swh_api_endpoint}/{self.vault_type}/{self.prefix}{self.project_id}/")

        #file_entry = cache.put(self.project_id,tar_file)
        inmemory_tar_file = io.BytesIO(tar_file_data.content)
        
        self.file_list = unpackProjectFile(self.project_id,inmemory_tar_file)

        #scheduleFileOnStream("",self.queue_list,file_list)

        return inmemory_tar_file

