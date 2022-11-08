<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use App\Models\Run;
use Symfony\Component\Process\Exception\ProcessFailedException;
use Symfony\Component\Process\Process;
use Illuminate\Support\Facades\Http;

class SparkSubmit extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'spark:submit {jar} {application} {run}';

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
        // Send Recipes
        $response = Http::contentType("application/json")->post('http://127.0.0.1:5000/test', [
            'body' => ['test' => 'prova']
        ]);

        $arguments = $this->arguments();
        $jar = $arguments['jar'];
        $application = $arguments['application'];
        $run_id = $arguments['run'];

        $run = Run::find($run_id);


        $process = new Process(['/root/spark-3.1.2-bin-hadoop3.2/bin/spark-submit', '--name',  $application, '--class',  'SimpleApp', '--master',  'spark://116.202.18.200:7077',  '--executor-memory', '1G',  '--total-executor-cores', '2', '--deploy-mode', 'client', $jar ,'1', '127.0.0.1', '9999', '1']);
        $process->run();

        // executes after the command finishes
        if (!$process->isSuccessful()) {
            throw new ProcessFailedException($process);
        }

        $path = "/root/lab/Software-Heritage-Analytics/Webconsole/tmp/".$run->job_id.".txt";
        $file = fopen($path, "w");
        fwrite($file, $process->getOutput());
        fclose($file);
        $run->path = $path;
        $run->save();
        
    }
}
