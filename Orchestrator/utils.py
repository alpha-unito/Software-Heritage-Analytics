import tarfile
import requests
import base64
import os
from typing import Dict
from language_extensions import get_extension_by_language_name
import gzip
import csv
import logging
import logging.handlers
from datetime import datetime
import multiprocessing

class SingletonType(type):
    # meta class for making a class singleton
    def __call__(cls, *args, **kwargs):

        try:
            return cls.__instance
        except AttributeError:
            cls.__instance = super(SingletonType, cls).__call__(*args, **kwargs)
            return cls.__instance



class Logger(object):
 
     # singleton class
    __metaclass__ = SingletonType

    def __init__(self,*args, **kwargs):
 
        #log_time = datetime.now().strftime("%Y%m%d-%H:%M:%S")
        log_time = ""
        self.write_to_file = True
        app_name = args[0]
        if self.write_to_file is True:
            self.file_name = f"logs/APP-{app_name}-{log_time}.log"
            

    def debug(self,header,msg=""):
        time = datetime.now().strftime("%H:%M:%S.%f")
        log_msg = f"[{time} {multiprocessing.current_process().name}] {header} {msg}\n"
        if self.write_to_file is True:
            with open(self.file_name, 'a+') as f:
                f.write(log_msg)
        else:
            print(log_msg)



def unpackProjectFile(project_id,tar_file):
        ret = {} 
        with tarfile.open(fileobj=tar_file,mode="r:gz" ) as file_list:
            for f in file_list:
                #print(file.name)
                try:
                    ret[f.name] = {'project_id':project_id,
                                      'file_basename': os.path.basename(f.name),
                                      'file_name': f.name, 
                                      'file_type': "",
                                      'data': base64.b64encode(file_list.extractfile(f.name).read()).decode()}
                except:
                    pass
                
        return ret

def unpackProjectFileData(project_id,tar_file_data):
        ret = {}
        file_data = gzip.decompress(tar_file_data)
        tarfile.TarInfo.frombuf(tar_file_data )
        with tarfile.open(project_entry,"r:gz" ) as file_list:
            for f in file_list:
                #print(file.name)
                try:
                    ret[f.name] = {'project_id':project_id,
                                      'file_basename': os.path.basename(f.name),
                                      'file_name': f.name,
                                      'file_type': "",
                                      'data': base64.b64encode(file_list.extractfile(f.name).read()).decode()}
                except:
                    pass

        return ret


def scheduleFileOnStream(language,queue_list,file_list: Dict):
    q_idx = 0
    ret = ""
    ret += "-----------------------------------\n"    
    #print("[+] Scheduling file for send (language:" + language + ")")
    extensions = ""
    num_file = 0
    data_size = 0
    ret += "[+] list "  + str(len(file_list)) + " files\n" 
    #print("[+] list "  + str(len(file_list)) + " files")
    if language != "":
        extensions = get_extension_by_language_name(language)
    for file, file_data in file_list.items():
        filename, file_extension = os.path.splitext(file)
        #print("[+] stream:"+ str(q_idx) + " " + file)
        if extensions != "":
            try:
                extensions.index(file_extension)
            except:
                #print("[+] PRUNED " + file)
                continue
        num_file = num_file + 1
        data_size += len(file_data) 
        #print("[+] stream:"+ str(q_idx) + " " + file)
        #print("[+] stream:"+ str(q_idx) + " " + str(file_data))
        queue_list[q_idx].put(file_data.copy())
        #print(queue_list[q_idx])
        #print(queue_list[q_idx].qsize())
        
        q_idx = (q_idx + 1) % len(queue_list)
    #print("[+] stream "  + str(num_file) + " files")    
    ret += "[+] stream "  + str(num_file) + " files, tot byte"+str(data_size)+"\n"+"-----------------------------------\n"
    #print("-----------------------------------")
    return ret

def logInfo(call_time,op, project_id, outcome, execution_time, error):
    log_info = dict()
    log_info["self.project_id"] = project_id
    log_info["call_time"] = call_time
    log_info["op"] = op
    log_info["outcome"] = outcome
    log_info["error"] = error
    log_info["execution_time"] = execution_time - log_info["call_time"] 
    return log_info

def logCSV(appname,log_string_dict):
    #return
    
    with open(f'logs/{appname}.csv', mode='a+') as csv_file:
        fieldnames = ['call_time', 'op', 'project_id', 'outcome', 'execution_time', 'error' ]
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
     
        writer.writerow(log_string_dict)
    

