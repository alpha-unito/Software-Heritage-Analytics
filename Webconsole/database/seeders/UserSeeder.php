<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;

class UserSeeder extends Seeder
{
    /**
     * The administrators to be seeded.
     *
     * @var array
     */
    protected $users = [
        ['name' => 'user', 'email' => 'user@admire.test'],
    ];

    /**
     * Run the database seeds.
     *
     * @return void
     */
    public function run()
    {
        foreach ($this->users as $user) {
            User::factory()->create($user);
        }
    }
}
