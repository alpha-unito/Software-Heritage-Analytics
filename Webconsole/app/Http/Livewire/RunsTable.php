<?php

namespace App\Http\Livewire;

use App\Models\Run;
use Livewire\Component;

class RunsTable extends Component
{
    public function render()
    {
        $runs = Run::orderby('created_at', 'DESC')->get();
        return view('livewire.runs-table', ['runs' => $runs]);
    }

    public function delete(Run $run){
        $run->delete();
    }

    public function run(Run $run){
        // $run->run();
        $application_name = $run->application->name;
        $jar = $run->application->fullPath();
        return redirect()->route('run.spark', ['run' => $run->id, 'jar' => $jar, 'application_name' => $application_name]);
    }

    public function inspect(Run $run){
        // dd($run->path);
        return redirect()->route('run.inspect', ['run' => $run->id]);
    }
}
