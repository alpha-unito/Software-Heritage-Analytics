<?php

namespace App\Http\Livewire;

use App\Models\Application;
use App\Models\Recipe;
use App\Models\Run;
use Livewire\Component;

class RunsBuilder extends Component
{
    public $settings;
    public $selected_application;
    public $selected_recipe;


    public function render()
    {
        $applications = Application::all()->where('user_id', Auth()->user()->id);
        $recipes = Recipe::all()->where('user_id', Auth()->user()->id);
        return view('livewire.runs-builder', ['applications' => $applications, 'recipes' => $recipes]);
    }
}
