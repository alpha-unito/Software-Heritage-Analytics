<?php

namespace App\Models;

use Illuminate\Support\Str;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Facades\Storage;

class Application extends Model
{
    use HasFactory;
    protected $casts = ['tags' => 'array'];

    public static function boot()
    {
        parent::boot();
        self::creating(function ($model) {
            $model->uuid = (string) Str::uuid()->toString();
        });

        self::deleting(function ($model) {
            Storage::disk('local')->delete('applications/'.$model->uuid);
        });
    }

    public function download() {
        return Storage::disk('local')->download('applications/'.$this->uuid, $this->name.".jar");
    }

    public function user() {
        return $this->belongsTo(User::class);
    }

    public function runs() {
        return $this->hasMany(Run::class);
    }

}
