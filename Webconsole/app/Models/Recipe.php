<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Recipe extends Model
{
    use HasFactory;

    protected $casts = ['tags' => 'array', 'data' => 'array'];

    protected $fillable = ['data', 'name'];

    public function user() {
        return $this->belongsTo(User::class);
    }

    public function runs() {
        return $this->hasMany(Run::class);
    }
}
