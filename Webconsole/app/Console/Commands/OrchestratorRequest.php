<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use App\Models\Run;

Use Exception;

class OrchestratorRequest extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'orchestrator:request {run} {app_name} {app} {rules} {projects*}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Command description';

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
        $run = $arguments['run'];
        $app_name = $arguments['app_name'];
        $app = $arguments['app'];
        $rules = $arguments['rules'];
        $projects = $arguments['projects'];
        $run = Run::find($run)->first();

        $address = 'localhost';
        $service_port = 4320;

        $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        socket_set_option($socket,SOL_SOCKET, SO_RCVTIMEO, array("sec"=>5, "usec"=>0));
        try{
            socket_connect($socket, $address, $service_port);
        } catch (Exception $e) {
            $run->info = "ERROR";
            $run->save();
            return Command::FAILURE;
        }

        if ($socket === false) {
            $run->info = "ERROR";
            $run->save();
            return Command::FAILURE;
        }

        $template = '{
            "app_name": "%s",
            "app" :"%s",
            "projects":%s,
            "rules" : %s
        }';

        $projects_string = '{';
        foreach ($projects as $key=>$project){
            if($key != 0) $projects_string = $projects_string.',' ;
            $projects_string = $projects_string."\"".$project."\":{\"language_type\":\"C++\"}";
        }
        $projects_string = $projects_string.'}';

        $jsonString = sprintf($template, $app_name, $app, $projects_string, str_replace(["\r","\n","\t"], "", $rules ));

        $sent = socket_write($socket, $jsonString);
        if($sent === false) {
            $run->info = "ERROR";
            $run->save();
            return Command::FAILURE;
        }
        if(socket_recv($socket, $replay,4096, 0)){
            $run->info = $replay;
            $run->save();
            return Command::SUCCESS;
        } else {
            $run->info = "ERROR";
            $run->save();
            return Command::FAILURE;
        }

    }
}
