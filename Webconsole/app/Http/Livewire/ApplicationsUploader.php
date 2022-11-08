<?php

namespace App\Http\Livewire;

use App\Models\Application;
use Livewire\Component;
use Livewire\WithFileUploads;
use Livewire\WithPagination;

class ApplicationsUploader extends Component
{
    use WithFileUploads;

    public $applciation;

    public $file;
    public $tags;
    public $description;
    public $name;
    public $application_class;
    public $enableButton = false;
    public $iteration = 0;

    public function render()
    {
        $applications = Application::all();
        return view('livewire.applications-uploader', ['applications' => $applications]);
    }

    public function delete(Application $application)
    {
        if(count($application->runs) == 0){
            $application->delete();
            return true;
        }
        return false;
    }

    public function nextUpload() {
        $name = $this->file->getClientOriginalName();
        $this->file->delete();
        $this->file = null;
        return $name;
    }

    public function save() {
        if($this->file !== null) {
            $application = Application::make();
            $application->name = $this->name;
            $application->description = $this->description;
            $application->application_class = $this->application_class;
            $application->tags = array_filter(explode(",", str_replace(' ', '', $this->tags)));
            $application->user()->associate(auth()->user());
            $application->save();
            $this->file->storeAs('applications', $application->uuid, 'local');
        }
    }

    public function clean() {
        $this->iteration++;
        $this->file = null;
        $this->tags = null;
        $this->description = null;
        $this->name = null;
        $this->application_class = null;
        return true;
    }
}
