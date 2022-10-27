<?php

namespace App\Http\Controllers;

use App\Models\Application;
use App\Models\Recipe;
use App\Models\Run;
use Illuminate\Http\Request;

class RunController extends Controller
{
    public function index()
    {
        return view('runs.index');
    }

    public function create()
    {
        return view('runs.create');
    }

    public function store(Request $request) {
        $run = Run::make();
        $run->settings = $request->input('settings');
        $run->user()->associate(Auth()->user());
        $run->recipe()->associate($request->input('recipe'));
        $run->application()->associate($request->input('application'));
        $run->save();
        return redirect()->route('run.index');
    }
}
