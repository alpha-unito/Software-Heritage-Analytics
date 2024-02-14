import os
import sys
from CacheRequest import CacheRequest
from utils import *
from multiprocessing import *
from config import _CONFIG
import traceback

def CacheWorker(app_name, q: JoinableQueue, stop_event: Event, q_stream: Queue):

    pid = os.getpid()
    print(f"[**CW ({app_name})] Cache Worker process pid: {pid}")

    logger = Logger(app_name)

    num_req = 0
    logger.debug(f"Starting CacheWorker ({current_process().name})...")
    cache_req = None
    while True:
        if stop_event.is_set():
            logger.debug("CacheWorker exiting because of stop_event")
            break
        # We set a timeout so we loop past "stop_event" even if the queue is empty
        try:
            cache_req = q.get()
            print(f"[**CW_{pid} ({app_name})] Has extracted from queue")
            # cache_req = q.get(timeout=.05)
            # cache_req = CacheRequest(cache_req_params["app_name"],stream_queue_list,cache_req_params["project_id"],cache_req_params["language_type"])
        except Exception as e:
            # Run next iteration of loop
            print(e)
            print(f"[**CW_{pid} ({app_name})] Empty requestform queue")
            q.task_done()
            continue

        # Exit if end of queue
        # if cache_req_params is None:
        if cache_req == "END":
            print(f"[**CW_{pid} ({app_name})] find 'None' on queue")
            logger.debug("CacheWorker exiting: find 'None' on queue")
            q.task_done()
            break

        # Do the task
        try:
            file_list = cache_req.run()

            num_req += 1
            print(
                f"[**CW_{pid} ({app_name})] Req: {cache_req.project_id}" + str(num_req))
            logger.debug(f"run cache request {cache_req.project_id}")
            ret = scheduleFileOnStream(
                cache_req.project_lang, q_stream, file_list)
            logger.debug(ret)
        except Exception as e:

            print(
                f"[**CW_{pid} ({app_name})] Failed to run process request {cache_req.project_id}:" + str(e))
            print(traceback.format_exc())
            ret = scheduleFileOnStream(
                cache_req.project_lang, q_stream, list())
            # q.task_done()
            # break
            raise RuntimeError(f"Error pid: {pid}")

            # q.task_done()
            # break
            # Can implement some kind of retry handling here

        # finally:
        #    logger.debug(f"Exit CacheWorker ({current_process().name})")
        q.task_done()

    print(
        f"[**CW_{pid} ({app_name})] EXIT Cache Worker process [num req done:{num_req}]")
    sys.exit()


# class CacheManager(Thread):
class CacheManager():
    def __init__(self, app_name, queue_list, project_list, max_num_process, dry_run):
        # Thread.__init__(self)
        self.app_name = app_name
        self.queue_list = queue_list
        self.project_list = project_list
        self.retrieveThread_list = []
        self.cacheProcess_list = []
        self.request_queue = JoinableQueue(maxsize=1)
        self.stop_event = Event()
        self.log_header = f"[**CM ({self.app_name})] "
        self.max_num_cache_request = max_num_process * \
            _CONFIG["ratio_spark_thread_cache_request"]
        if _CONFIG["max_num_cache_request"] > 0:
            self.max_num_cache_request = _CONFIG["max_num_cache_request"]

        if self.max_num_cache_request > len(self.project_list):
            self.max_num_cache_request = len(self.project_list)

        # self.logging = setup_logger('CTRL',f"logs/APP-{self.app_name}.log")
        self.dry_run = dry_run
        self.logging = Logger(app_name)

        self.logging.debug(
            f"{self.log_header} Cache Manager Pool started [max req:{self.max_num_cache_request}]")

    def run(self):
        pid = os.getpid()
        print(f"{self.log_header} Cache Manager process pid: {pid}")

        # Create a cache precess pool
        self.logging.debug(
            f"Create cache request process pool ({self.max_num_cache_request} processes)")
        print(f"{self.log_header}Create cache request process pool ({self.max_num_cache_request} processes)")
        for i in range(self.max_num_cache_request):
            p = Process(name=f"CacheWorker-{self.app_name}-{i:02d}", daemon=False, target=CacheWorker, args=(
                self.app_name, self.request_queue, self.stop_event, self.queue_list))
            self.cacheProcess_list.append(p)
            p.start()
        self.logging.debug(
            f"[+] Run {self.max_num_cache_request} process in pool")

        self.logging.debug("[+] Project list:")

        # Create a cache Request task for every project in recipe
        for project_id in self.project_list:

            self.logging.debug("[+] new cache request " + project_id +
                               " (" + str(self.project_list[project_id]['language_type']) + ")")
            print(f"{self.log_header} new cache request " + project_id +
                  " (" + str(self.project_list[project_id]['language_type']) + ")")

            cache_ret = CacheRequest(
                self.app_name, project_id, self.project_list[project_id]['language_type'])
            self.request_queue.put(cache_ret)

        # Put exit command in every cache process pool
        for i in range(self.max_num_cache_request):
            self.logging.debug("[+] Set None on Cache worker task queue")
            print("[**]EXIT Cache worker")
            self.request_queue.put("END")

        # Wait until all tasks are processed
        print(f"{self.log_header} Wait for cache worker end...")
        self.logging.debug("[+] Wait for cache worker end...")
        self.request_queue.join()
        # self.logging.debug("All cache worker ended")

        # time.sleep(10)

        # Put exit command on spark streaming thread

        if not self.dry_run:
            for q in self.queue_list:
                print(f"{self.log_header} Set None on queue task streming")
                self.logging.debug("Set None on queue task streming")
                q.put(None)

        for cp in self.cacheProcess_list:
            self.logging.debug("Terminate cache worker...")
            cp.terminate()
            self.logging.debug("Join cache worker...")
            cp.join()

        self.logging.debug("Cache Manager exit")
        print(f"{self.log_header} Cache Manager exit")
        # os._exit(os.EX_OK)
        sys.exit()

    def handle_signal(self, signum):
        print(f"{self.log_header}  Cache Manager handle_signal event")
        return
