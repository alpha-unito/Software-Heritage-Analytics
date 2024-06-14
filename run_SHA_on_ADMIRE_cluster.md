
# Software Heritage Analytics Tutorial

This tutorial will guide you through the steps to configure and run the Heritage Analytics Framework Software on the Turin cluster called c3s. The final step of the guide shows how to run the license analytics application on a set of preconfigured projects.

## Step 1: Clone the Repository

First, clone the Software Heritage Analytics repository from GitHub:

```bash
git clone https://github.com/alpha-unito/Software-Heritage-Analytics
```


## Step 2: Add the Spack Repository

```bash
cd Software-Heritage-Analytics
```
Add the `sha` repository to Spack:

```bash
spack repo add sha
```

## Step 3: Install Required Packages

Install `sha` and `scancode-toolkit` using Spack:

```bash
spack install sha scancode-toolkit
```


## Step 4: Edit Configuration for storage directory
Navigate to the `Tools` directory:

```bash
cd Tools
```

At the head of the `sha_on_slurm.sh` file are three variables for configuring access to the storage directory:

`CACHEMIRE_DATA_DIR` : path to storage directory

`ENABLE_CACHEMIRE`: 1 or 0 for enable or disable the launch of the cachemire process

`CACHEMIRE_LD_PRELOAD_PATH`: path to library for preload

```bash
nano sha_on_slurm.sh
```

Replace the value of three variables with your preferred values path and save the file and run script

```bash
./sha_on_slurm.sh
```

## Step 4 (ALTERNATIVE): Run cachemire manually
If you wish to launch `cachemire` manually, you must configure ENABLE_CACHEMIRE=0 and launch `sha_on_slurm.sh`. 
```bash
./sha_on_slurm.sh
```

Go to the `Software-Heritage-Analytics/Cachemire` folder and compile `cachemire`
```bash
cd Software-Heritage-Analytics/Cachemire
make
```
To run `cachemire` manually you must first follow the instructions in Step 5 and then access the first allocated node (Master node e.g., `broadwell-029`) after which you can run the following commands
```bash
cd Software-Heritage-Analytics/Cachemire/bin
LD_PRELOD=yourpathtolib ./cachemire -d yourpathtodatastorage
```

## ADMIRE AD-HOC filesystems usage
The 'Cachemire' caching system is designed to seamlessly interface with various ad-hoc filesystems provided by Admire. The provided guide offers a comprehensive walkthrough for using Cachemire with Hercules, but it is equally applicable to other ad-hoc filesystems.
Once installation and configuration of one of the three filesystems are complete, you can start Cachemire in one of the following ways, according to the chosen filesystem:

### Hercules 
[Git repository](https://gitlab.arcos.inf.uc3m.es/admire/hercules)
```bash
LD_PRELOAD=<install_path>libhercules_posix.so ./cachemire -d yourpathtodatastorage
```

### GekkoFS
[Git repository](https://storage.bsc.es/gitlab/hpc/gekkofs)
```bash
LD_PRELOAD=<install_path>/lib64/libgkfs_intercept.so ./cachemire -d yourpathtodatastorage
```

### Expand
[Git repository](https://github.com/xpn-arcos/xpn)
```bash
LD_PRELOAD=<install_path>/xpn/lib/xpn_bypass.so ./cachemire -d yourpathtodatastorage
```

## Step 5: Configure and Run the SHA Script

It is possible to edit the parameters at the head of the run_sha.sh file to configure the launch of the framework. Below are the editable parameters:

#SBATCH --job-name SHAspark
#SBATCH --nodelist broadwell-[029-036]
#SBATCH -N 8
#SBATCH --ntasks-for-node 1
#SBATCH --cpus-for-task 1
#SBATCH --mem 0 
#SBATCH --exclusive

The important parameters are -N for the number of nodes and --nodelist for the list of node names.

After configuration can execute the SHA script:

```bash
sbatch ./run_sha.sh
```

## Step 6: Connect to the first Node allocated

SSH into the first node (e.g., `broadwell-029`):

```bash
ssh broadwell-029
```

## Step 7: Load Installed Packages

Load the `sha`:

```bash
spack load sha
```

## Step 8: Set Spark Application Path

Set the `SPARK_APP_PATH` environment variable:

```bash
export SPARK_APP_PATH=~/Software-Heritage-Analytics/Example/
```

## Step 9: Run the Dashboard Client

Run the `dashboardclient` with the appropriate parameters:

```bash
dashboardclient -a broadwell-029 -p 4320 -r ~/Software-Heritage-Analytics/Example/recipe_test.json -m broadwell-029 -n licensectrl -dir ~/Software-Heritage-Analytics/Example/recipe_test -sb scancode -si ~/Software-Heritage-Analytics/Example/scancode_index.json -rp ~/Software-Heritage-Analytics/Example/ramdisk -gp ~/Software-Heritage-Analytics/Example/grafo.txt -op ~/Software-Heritage-Analytics/Example/output -D
```

### Explanation of Parameters:
- `-a broadwell-029`: Specifies the address of the orchestrator node.
- `-p 4320`: Port number of orchestrator.
- `-r ~/Software-Heritage-Analytics/Example/recipe_test.json`: Path to the JSON recipe file.
- `-m broadwell-029`: Specifies the address of the Spark Master node.
- `-n licensectrl`: Name of the .jar application to execute.
- `-dir ~/Software-Heritage-Analytics/Example/recipe_test`: Directory path for the job.
- `-sb scancode`: Specifies the scanner backend.
- `-si ~/Software-Heritage-Analytics/Example/scancode_index.json`: Path to the scancode index file.
- `-rp ~/Software-Heritage-Analytics/Example/ramdisk`: Path to the RAM disk.
- `-gp ~/Software-Heritage-Analytics/Example/grafo.txt`: Path to the graph file.
- `-op ~/Software-Heritage-Analytics/Example/output`: Path to the output directory.
- `-D`: Enables debug mode.

Wait for application end.
