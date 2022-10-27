<?php

namespace App\Http\Livewire;

use App\Models\Recipe;
use Illuminate\Support\Facades\Auth;
use Livewire\Component;

class RecipesTable extends Component
{
    public function render()
    {
        $recipes = Recipe::all()->where('user_id', Auth()->user()->id);
        return view('livewire.recipes-table', ['recipes' => $recipes]);
    }

    public function delete(Recipe $recipe){
        if(count($recipe->runs) == 0){
            $recipe->delete();
            return true;
        }
        return false;
    }
}
