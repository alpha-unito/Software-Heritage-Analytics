import json
import sys
import socket
import time
from config import _CONFIG
import uuid

# logging.basicConfig(level=logging.DEBUG,format='[%(levelname)s] (%(threadName)-9s) %(message)s',)
ext_to_exclude = [
    ".jpg", ".jpeg", ".png", ".gif", ".tiff",
    ".mp3", ".wav", ".flac",
    ".mp4", ".mov", ".avi", ".mkv", ".webm", ".wmv", ".flv", ".mpeg", ".ogg", ".dvx", ".rm", ".asf", ".3pg"
    ".doc", ".docx", ".pdf",
    ".ppt", ".pptx", ".key",
    ".xls", ".xlsx", ".csv",
    ".zip", ".rar", ".7z",
    ".html", ".htm",
    ".json",
    ".xml",
    ".csv"
]


class SparkRequest():

    def __init__(self, conn, ip, port, q):
        # Thread.__init__(self)
        self.ip = ip
        self.port = port
        self.conn = conn
        self.q = q
        self.conta = 0
        self.id = uuid.uuid4()
        # print(self.q)
        # print(self.q.qsize())
        print("[**] New server socket thread started for spark connetion from " +
              ip + ":" + str(port))

    def run(self):

        while True:
            data_file = None
            if not self.q.empty():
                data_file = self.q.get_nowait()
                if (data_file == "END"):
                    print("[**] Spark request - END")
            try:
                # print("[**] " + data_file)
                if data_file == "END":
                    end_msg = {'project_id': 'EOS', 'file_name': '',
                               'file_type': '', 'data': ''}
                    self.conn.send(json.dumps(end_msg).encode() + b'\n')
                    print("[**] Data is END... EOS")
                    self.conn.close()
                    break
                if data_file is None:
                    self.conta += 1
                    if (self.conta == 10000):
                        time.sleep(5)
                        self.conta = 0
                    time.sleep(_CONFIG["send_sleep_wait"])
                    continue
                if any((data_file["file_basename"]).endswith(ext) for ext in ext_to_exclude):
                    continue
                time.sleep(_CONFIG["send_sleep_debug"])
                msg = json.dumps(data_file)

                self.conn.send(msg.encode() + b'\n')

                # print(f"{project_id} - {name}")
            except:
                print("[**] Unexpected socket error - Spark worker has close socket")
                self.conn.close()
                sys.exit()

        while True:
            try:
                result = self.conn.getsockopt(
                    socket.SOL_SOCKET, socket.SO_KEEPALIVE)
                if result == 0:
                    print('Il socket è stato chiuso dal client')
                    time.sleep(10)
                    self.conn.close()
                    break
                else:
                    print('Il socket è ancora aperto')

            except:
                print("[**] Spark worker has close socket")
                self.conn.close()
                return

        return
