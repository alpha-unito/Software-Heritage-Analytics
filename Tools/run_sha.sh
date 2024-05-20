#!/bin/bash

## TODO: edit file header

#SBATCH --job-name SHAspark
##SBATCH --reservation ReservationName
#SBATCH --nodelist broadwell-[029-036]
#SBATCH -N 8
#SBATCH --ntasks-per-node 1
#SBATCH --cpus-per-task 1
#SBATCH --mem 0 # 0 means use all available memory (in MB)
#SBATCH --exclusive

srun bash sha_on_slurm.sh
