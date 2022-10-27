<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRunsTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('runs', function (Blueprint $table) {
            $table->id();
            $table->json('settings')->nullable();
            $table->tinyInteger('status')->default(0);
            $table->integer('counter_executions')->default(0);
            $table->foreignId('user_id');
            $table->foreignId('recipe_id');
            $table->foreignId('application_id');
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('runs');
    }
}
