<?php

namespace App\Http\Controllers;

class RecipeController extends Controller {

    public function index() {
        return view('recipes.index');
    }

    public function create() {
        return view('recipes.create');
    }
}
