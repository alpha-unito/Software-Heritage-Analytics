<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;

class OrchestratorRequest extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'orchestrator:request';

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
        $address = 'localhost';
        $service_port = 4320;

        $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        try{
            socket_connect($socket, $address, $service_port);
        } catch (Throwable $e) {
            echo "Exception -> ".$e;
            return;
        }
        if ($socket === false) {
            echo "FAILED: " . socket_strerror(socket_last_error()) . "\n";
        } else {
            echo "OK.\n";
        }

        $jsonString = '{"app_name":"cavallo", "prova":15}';

        $sent = socket_write($socket, $jsonString);
        if($sent === false) {
            echo "SEND FAILD! \n";
        } else {
            echo "OK.\n";
        }
        return 0;
    }
}
