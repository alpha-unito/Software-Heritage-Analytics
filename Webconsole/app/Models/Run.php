<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Ramsey\Uuid\Uuid;

class Run extends Model
{
    use HasFactory;

    protected $casts = ['settings' => 'array'];

    public function application(){
        return $this->belongsTo(Application::class);
    }

    public function recipe(){
        return $this->belongsTo(Recipe::class);
    }

    public function user() {
        return $this->belongsTo(User::class);
    }

    public function reset() {
        $this->job_id = null;
        $this->path = null;
        $this->info = null;
        $this->status = 0;
        $this->save();
    }

    public function run() {
        // CAPIO
        $port = "4320";
        $host = "c3snodo4";

        $spark = "C3SNodo1.maas";

        $tmp_recipe = Uuid::uuid4();

        $full_recipe = [];
        $full_recipe['app'] = "simple-project_2.12-1.0.jar";
        $full_recipe['rules'] = [
            "num_slave" => 2,
            "dstream_time" => 1000
        ];
        $full_recipe["projects"] = [];

        foreach($this->recipe->data as $project) {
            $full_recipe["projects"][$project] = [
                "language_type" => ""
            ];
        }
        dd("/usr/bin/python3 dashboardclient.py -p $port -a $host -m $spark -r /tmp/$tmp_recipe.json -e 20G", json_encode($full_recipe));
    }
}
