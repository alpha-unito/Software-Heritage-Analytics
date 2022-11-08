<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddExecutionTimeToRuns extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('runs', function (Blueprint $table) {
            $table->dateTime('execution_time')->nullable();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('runs', function (Blueprint $table) {
            $table->dropColumn('execution_time');
        });
    }
}
