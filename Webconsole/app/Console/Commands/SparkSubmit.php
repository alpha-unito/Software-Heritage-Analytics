<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Symfony\Component\Process\Exception\ProcessFailedException;
use Symfony\Component\Process\Process;

class SparkSubmit extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'spark:submit';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Run spark submit';

    /**
     * Create a new command instance.
     *
     * @return void
     */
    public function __construct()
    {
        parent::__construct();
    }

    /**
     * Execute the console command.
     *
     * @return int
     */
    public function handle()
    {
        $arguments = $this->arguments();

        // $process = new Process(['/home/xtrust/lab/spark/spark-3.1.2-bin-hadoop3.2/bin/spark-submit', '--name',  'app1', '--class',  'SampleApp', '--master',  'spark://116.202.18.200:7077',  '--executor-memory', '1G',  '--total-executor-cores', '1', '--deploy-mode', 'client', '/home/xtrust/lab/spark/sparking/scala/test_t/target/scala-2.12/simple-project_2.12-1.0.jar' ,'1', '127.0.0.1', '9999', '1']);
        $process = new Process(['ls', '-la']);
        $process->run();
        // executes after the command finishes
        if (!$process->isSuccessful()) {
            throw new ProcessFailedException($process);
        }
        $this->info($process->getOutput());
        $this->info('finito');
        sleep(5);
        // return $ret;
    }
}
