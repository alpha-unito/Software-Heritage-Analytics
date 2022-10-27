<?php

namespace App\Http\Livewire;

use App\Models\Run;
use Livewire\Component;

class RunsTable extends Component
{
    public function render()
    {
        $runs = Run::all();
        return view('livewire.runs-table', ['runs' => $runs]);
    }

    public function delete(Run $run){
        $run->delete();
    }

    public function run(Run $run){
        // $run->run();
        return redirect()->route('run.spark');

    }
}
