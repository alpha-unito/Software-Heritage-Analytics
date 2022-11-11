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

    public function inspect(Run $run)
    {
        $output = file_get_contents($run->path, FALSE);
        $output = str_replace(" ", "&nbsp;", $output);
        return view('runs.inspect', ['output' => $output]);
    }

    public function store(Request $request) {
        $run = Run::make();
        $run->language = $request->input('language');
        $run->settings = $request->input('settings');
        $run->user()->associate(Auth()->user());
        $run->recipe()->associate($request->input('recipe'));
        $run->application()->associate($request->input('application'));
        $run->save();
        return redirect()->route('run.index');
    }
}
