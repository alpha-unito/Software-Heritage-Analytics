<?php

namespace App\Http\Controllers;

use App\Models\Application;
use Illuminate\Http\Request;

class ApplicationController extends Controller
{
    public function index() {
        return view('applications.index');
    }

    public function download(Application $application){
        return $application->download();
    }

}
