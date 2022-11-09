
SoftWare Heritage Analytics Framework

**Orchestration layer**

Data orchestration is the process of taking isolated data from different data storage locations, combining and organizing them, and making them available for data analysis tools. In this case, the task of the orchestrator is to take projects from heritage software and distribute them to Apache Spark workers on which the data anlysis applications run. 

The orchestrator can handle multiple streams of data to supply multiple applications. Each application must provide a configuration file where it specifies the list of projects it is interested in. This file is called a "recipe." In addition to the list of projects, the file contains other congifuration parameters such as the number of workers to be used and the size of memory to be allocated.


**Recipes**

A recipe is a simple json file. This is an example of a recipe:

``` json
{
	"app_name": "test1",
	"app" :"simple-project_2.12-1.0.jar",
	"projects":{
		"028b0486a38525a7b5d0108725b2b94abdbed409" : {"language_type":"C++"},
		"3b2caeea51d34f766f5fcef27a9770323c9aa60f" : {"language_type":"C++"},
		"61b3733be05b2c986c8797ef8fabf09976151eb2" : {"language_type":"C++"},
		"0529771a960cd823d20e5d28617ffa8f0fc060ac" : {"language_type":"C++"},
		"e86b3135ff4452997a55f231212a3fb6e5f8aa9d" : {"language_type":"C++"},
		"4780f204730186a5421851d404872910f3e6b5e3" : {"language_type":"C++"},
		"bf79c49fd9440c4a7e554b0315d0e6e14ca71fd9" : {"language_type":"C++"},
		"21d03bee59d127f9b9572ca8d3ca0c0f0dba846a" : {"language_type":"C++"},
		"ebe7c2e1193a5518fffbf0a09bb54c80937e0e23" : {"language_type":"C++"},
		"266b4ea87d2ac441bc02ad2c4ba2c4f332c7c0ce" : {"language_type":"C++"}
	},
	"rules" : {"num_slave":2 , "dstream_time": 1000}
}
```

app_name: Unique app identifier when APP is running

app: specifies the jar package in which the code (scale) of the APP is contained

projects: the list of software Heritage projects unique identifier to analyze

language_type: the files contents in each project can be filtered by a language type extension

rules: groups parmaeters regarding the app's performance at runtime, such as number of workers, ram, etc.

**Language filtering**

The json language_extensions.py file lists all the recognized file formats for specific progamming language to do filtering of the files contained in the projects. Below is an example of how to specify a new extension for a language:

``` json
  {
      "name": "language_name",
      "type": "little_description",
      "extensions":[
         ".extension_1",
         ".extension_2",
         ".extension_3"
      ]
   }
```

**Configuration**

The orchestrator module is a daemon server that is listening on a specific port (default: 4320). The server can be configured by means of the file config.py ch which contains a number of parameters specified below:

``` json
_CONFIG = {
        "port": 4320,
	"bind_address" : "0.0.0.0",
        "spark_client_base_port": 4321,
        "buffer_size" : 100000,
        "swh_api_endpoint": "https://archive.softwareheritage.org/api/1/vault",
        "swh_polling_time": 3,
        "swh_vault_type" : "directory",
        "swh_prefix" : "",
        "send_sleep_debug": 0.0001,
        "num_max_app_client": 5,
        "default_cache_ip": "127.0.0.1",
        "default_cache_port": 13000,
        "max_num_cache_request": 0,
        "ratio_spark_thread_cache_request": 2
        }
```


**Install and run**

To install the server, simply clone this project and edit edit the configuration file and run with the following command:

```
python3 pycachemire
```

The output show this:
```
PyCachemire server : Waiting for connections from Dashboard clients on port 4320 ...
```


**Run app from shell**

The dashboard.py script can be used to run an analysis application. The script provides the following commands: 

```
python3 dashboardclient.py -h
usage: dashboardclient.py [-h] [-a A] [-p P] [-r R] [-n N] [-d [D]] [-m M] [-e E]

optional arguments:
  -h, --help  show this help message and exit
  -a A        Server IP address
  -p P        Server Port
  -r R        Recipe json file
  -n N        Set App name
  -d [D]      Activate spark dry run mode.
  -m M        Set spark master address
  -e E        Set spark memery
```

This is an example for run application:

```
python3 dashboardclient.py -a 127.0.0.1 -p 4320 -m 127.0.0.1 -r app/recipe0.json
```
