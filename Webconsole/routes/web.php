<?php

use App\Http\Controllers\ApplicationController;
use Illuminate\Support\Facades\Route;

use App\Http\Controllers\RecipeController;
use App\Http\Controllers\RunController;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Http\Request;


/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/

Route::get('/', function () {
    return redirect('/dashboard');
});

Route::middleware([
    'auth:sanctum',
    config('jetstream.auth_session'),
    'verified'
])->group(function () {
    Route::get('/dashboard', function () {
        return view('dashboard');
    })->name('dashboard');
    Route::get('/configs', function () {
        return view('dashboard');
    })->name('configs');
    Route::get('/applications', [ApplicationController::class, 'index'])->name('application.index');
    Route::get('/applications/{application}/download', [ApplicationController::class, 'download'])->name('application.download');
    Route::get('/recipes', [RecipeController::class, 'index'])->name('recipe.index');
    Route::get('/recipes/create', [RecipeController::class, 'create'])->name('recipe.create');
    Route::get('/runs', [RunController::class, 'index'])->name('run.index');
    Route::get('/runs/create', [RunController::class, 'create'])->name('run.create');
    Route::post('/runs/create', [RunController::class, 'store'])->name('run.store');
    Route::get('/runs/{run}/inspect', [RunController::class, 'inspect'])->name('run.inspect');
    Route::get('/run', function (Request $request) {
        // $exitCode = Artisan::call('spark:submit', []);
        \App\Jobs\SparkSubmitJob::dispatch($request->input());
        sleep(2);
        return redirect()->route('run.index');
    })->name('run.spark');
});
