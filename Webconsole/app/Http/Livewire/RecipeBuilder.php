<?php

namespace App\Http\Livewire;

use App\Models\Recipe;
use Carbon\Carbon;
use Livewire\Component;
use Illuminate\Support\Facades\Http;

class RecipeBuilder extends Component
{
    public $query = '';

    public $results = [];
    public $selected = [];
    public $cache = [];
    public $selectAll = false;

    public $info = [];

    public $branches = [
    ];

    public $selectedProject = null;
    public $phase = 0;
    public $full = true;

    public $projects = [];

    public $name = "";
    public $recipe = null;
    public $tags = "";
    public $description = "";

    public function search() {
        $response = Http::get("http://dsm01.di.unito.it:5080/api/1/origin/search/$this->query/");
        $this->results = $response->json() ?? [];
        if(count($this->results) > 0) {
            foreach($this->results as $key => $result) {
                $crc32 = crc32($result['url']);
                $this->cache[$crc32] = $result['url'];
                $this->results[$key]['crc32'] = $crc32;
            }
        }
    }
    public function updatedQuery() {
        if($this->phase == 0) {
            $this->clearCache();
            $this->search();
        }
    }

    public function updatedSelectAll() {
        foreach($this->results as $result) {
            $this->selected[$result['crc32']] = $this->selectAll;
        }
    }

    public function nextPhase() {
        $this->phase++;
        $this->clearCache();
        $this->dispatchBrowserEvent('trigger-next', ['cache' => array_keys($this->cache)]);
    }

    public function clearCache() {
        foreach($this->cache as $key => $cached) {
            if(!array_key_exists($key, $this->selected)) {
                unset($this->cache[$key]);
            }elseif($this->selected[$key] === false) {
                unset($this->cache[$key]);
            }
        }
    }

    public function loadInfo($crc32) {
        $url = $this->cache[$crc32];
        $response = Http::get("http://dsm01.di.unito.it:5080/api/1/origin/$url/visit/latest");
        $info = $response->json();
        if(array_key_exists('exception', $info)) {
            unset($this->cache[$crc32]);
            return [];
        }
        $info['date'] = Carbon::parseFromLocale($info['date'])->format("M d, Y");
        $this->info[$crc32] = $info;
        $this->projects[$crc32] = [];
        return $info;
    }

    public function loadHead($crc32) {
        // dd($crc32);
        $url = $this->info[$crc32]['snapshot_url'];
        if(!array_key_exists($crc32, $this->branches)) {
            $response = Http::get("$url");
            $branches = $response->json();
            if($branches) {
                if(array_key_exists('branches', $branches)) {
                    $this->branches[$crc32] = $branches['branches'];
                }else{
                    unset($this->cache[$crc32]);
                    unset($this->info[$crc32]);
                    return false;
                }
            }else{
                return false;
            }

        }
        if(count($this->branches[$crc32]) > 0) {
            if(!array_key_exists('HEAD', $this->branches[$crc32])) {
                unset($this->cache[$crc32]);
                unset($this->info[$crc32]);
                return false;
            }
            $head = $this->branches[$crc32]['HEAD'];
            if($head['target_type'] == 'alias') {
                $target = $this->branches[$crc32][$head['target']]['target'];
            } else{
                $target = $head['target'];
            }
            $this->projects[$crc32][$target] = true;
        } else {
            unset($this->cache[$crc32]);
            unset($this->info[$crc32]);
            return false;
        }
    }

    public function createRecipe() {
        $emptyProjects = [];
        foreach($this->projects as $crc32 => $branches) {
            if(array_key_exists($crc32, $this->info)) {
                if(count($branches) == 0 && (($this->full && $this->info[$crc32]['status'] == 'full') || !$this->full)) {
                    array_push($emptyProjects, $crc32);
                }
            }else {
                unset($this->projects[$crc32]);
            }
        }
        if(count($emptyProjects) > 0) {
            $this->dispatchBrowserEvent('create-recipe', ['cache' => $emptyProjects]);
        }else{
            $recipe = [];
            foreach($this->projects as $crc32 => $branches) {
                $recipe += $branches;
            }
            $recipe_array = array_keys($recipe);
            $recipe = Recipe::make();
            $recipe->name = "New Recipe";
            $recipe->data = $recipe_array;
            $recipe->user()->associate(auth()->user());
            $recipe->save();
            $this->recipe = $recipe;
            $this->dispatchBrowserEvent('rename-recipe');
        }

    }

    public function loadBranches($crc32) {
        $url = $this->info[$crc32]['snapshot_url'];
        if(!array_key_exists($crc32, $this->branches)) {
            $response = Http::get("$url");
            $branches = $response->json();
            $this->branches[$crc32] = $branches['branches'];
        }
        $this->selectedProject = $crc32;
    }

    public function rename(){

        $this->recipe->update(['name' => $this->name]);
        $this->recipe->tags = array_filter(explode(",", str_replace(' ', '', $this->tags)));
        $this->recipe->description = $this->description;
        $this->recipe->save();
        $this->recipe = null;
        $this->name = $this->tags = $this->description = "";
        return true;
    }

    public function render()
    {
        return view('livewire.recipe-builder');
    }
}
