<?php

namespace App\Jobs;

use App\Console\Commands\SparkSubmit;
use App\Models\Run;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldBeUnique;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Artisan;

class SparkSubmitJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;
    protected $details;
    public $timeout = 0;

    /**
     * Create a new job instance.
     *
     * @return void
     */
    public function __construct($details)
    {
        $this->details = $details;
    }

    /**
     * Execute the job.
     *
     * @return void
     */
    public function handle()
    {
        // print($this->job->getJobId().PHP_EOL);

        $run = Run::find($this->details['run']);
        $run->job_id = intval($this->job->getJobId());
        $run->save();
        Artisan::call('spark:submit', ['jar' => $this->details['jar'], 'application' => $this->details['application_name'], 'run' => $run->id]);
    }
}
