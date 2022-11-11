<?php

namespace App\Http\Livewire;

use App\Models\Application;
use App\Models\Recipe;
use App\Models\Run;
use Livewire\Component;
use Illuminate\Database\Eloquent\Collection;

class RunsBuilder extends Component
{
    public $settings;
    public $selected_application;
    public $selected_recipe;
    public $language;
    public $name;
    public $languages;
    public $options;
    public $selected_language;

    public function mount() {
        $this->languages = json_decode(file_get_contents('../languages_filter.json'), True)['languages'];
        $this->settings = sprintf("{\n\t%s:%d,\n\t%s:%d\n}","\"num_slave\"",2,"\"dstream_time\"", 1000);
        $this->options = new Collection();
    }

    public function updatedName() {
        $languages = collect($this->languages);
        $options = $languages->reject(function($option) {
            if($this->name == '') {
                return true;
            }
            $option =  (object) $option;

            return strpos($option->name, $this->name) === false && strpos(strtolower($option->name), strtolower($this->name)) === false && strpos(implode($option->extensions??[]), $this->name) === false && strpos(strtolower(implode($option->extensions??[])), strtolower($this->name)) === false;
        });
        $this->options = $options;
    }


    public function setLanguage($id){
        $this->ignore = true;
        $this->selected_language = $this->languages[$id];
        $this->name = $this->selected_language["name"];
        $this->dispatchBrowserEvent('close-suggestion');
    }


    public function render()
    {
        $applications = Application::all()->where('user_id', Auth()->user()->id);
        $recipes = Recipe::all()->where('user_id', Auth()->user()->id);
        return view('livewire.runs-builder', ['applications' => $applications, 'recipes' => $recipes, 'options' => $this->options]);
    }
}
