<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use App\Models\Run;
use Symfony\Component\Process\Exception\ProcessFailedException;
use Symfony\Component\Process\Process;
use Illuminate\Support\Facades\Http;
use Carbon\Carbon;

Use Exception;


class SparkSubmit extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'spark:submit {jar} {app} {app_name} {run}';

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
        $jar = $arguments['jar'];
        $app_name = $arguments['app_name'];
        $app = $arguments['app'];
        $run = $arguments['run'];

        try{
            $process = new Process(['/root/spark-3.1.2-bin-hadoop3.2/bin/spark-submit', '--name',  $app_name, '--class',  $app, '--master',  'spark://116.202.18.200:7077',  '--executor-memory', '1G',  '--total-executor-cores', '2', '--deploy-mode', 'client', $jar ,'1', '127.0.0.1', '9999', '1']);
            $process->run();
        } catch (Exception $e) {
            echo "test2";
            $run->info = "ERROR";
            $run->save();
            return Command::FAILURE;
        }

        $path = "/root/lab/Software-Heritage-Analytics/Webconsole/tmp/".$run->job_id.".txt";
        $file = fopen($path, "w");
        fwrite($file, $process->getOutput());
        fclose($file);
        $run->path = $path;
        $run->execution_time =  Carbon::parse(gmdate("H:i:s", Carbon::now()->diffInSeconds($run->execution_time)));
        $run->save();
        return Command::SUCCESS;
    }

    public function fails()
    {
        $arguments = $this->arguments();
        $run_id = $arguments['run'];
        $run = Run::find($run_id);
        $run->info = "ERROR";
        $run->save();
        return Command::FAILURE;
    }

}
