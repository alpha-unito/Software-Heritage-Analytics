<?php

namespace Database\Seeders;

use App\Models\Recipe;
use Illuminate\Database\Seeder;

class RecipeSeeder extends Seeder
{
    /**
     * Run the database seeds.
     *
     * @return void
     */
    public function run()
    {
        $recipeFile = fopen(base_path('./ricetta0.json'), 'r');
        $recipe = [];
        $recipe['data'] = str_replace(" ", "", str_replace("\n", "", stream_get_contents($recipeFile)));
        $recipe['name'] = 'default';
        $recipe['user_id'] = 1;
        Recipe::factory()->create($recipe);
    }
}
