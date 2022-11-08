#### Prereqisite

                    
* composer >= 2
* php >= 8
* npm >= 8
* node >= 16
* MariaDB >= 10.3

Required PHP extensions:
* common
* mysql
* xml
* xmlrpc
* curl
* gd
* mbstring
* zip

If all prerequisites are met:
Install composer and npm packages:
> ``` bash
> composer install
> npm install
> ```
Copy example env 
> ``` bash
> cp .env.example .env
> ```
Generate a key, this will sets the `APP_KEY` value in your .env file.:
> ``` bash
> php artisan key:generate`
> ```
Create a MySQL Database and a User
Fill MySQL section in .env with your connection information:
> ``` mysql
> DB_CONNECTION=mysql
> DB_HOST=127.0.0.1
> DB_PORT=3306
> DB_DATABASE=
> DB_USERNAME=laravel
> DB_PASSWORD=laravel
> ```
If connection parameters are correct you can create migration and fill with seeder data (if you want empty database run migration without `--seed` parameter):
> ``` bash
> php artisan migrate:fresh --seed
> ```
Default port is 8000, go to: http://localhost:8000 and login 
<details><summary>If you run seeder default login is:</summary>
Username: user@admire.test
Password: password
</details>
