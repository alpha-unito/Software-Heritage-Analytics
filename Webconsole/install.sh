#! /bin/bash
[ "$UID" -eq 0 ] || exec sudo "$0" "$@"
DEBIAN_FRONTEND=noninteractive
DIR=$PWD

# Php, Apache, Composer and Npm Installation
apt install software-properties-common -y
add-apt-repository ppa:ondrej/php
apt update -y
curl -sL https://deb.nodesource.com/setup_16.x | bash -
apt install php8.1 php8.1-common php8.1-mysql php8.1-xml php8.1-xmlrpc php8.1-curl php8.1-gd php8.1-mbstring php8.1-zip curl unzip apache2 libapache2-mod-php nodejs -y
php -r "copy('https://getcomposer.org/installer', '/tmp/composer-setup.php');"
php /tmp/composer-setup.php
php -r "unlink('/tmp/composer-setup.php');"
mv composer.phar /usr/local/bin/composer

# Apache Configuration
# systemctl enable apache2
service apache2 start
a2enmod rewrite
cp laravel.conf /etc/apache2/sites-available/laravel.conf
a2ensite laravel.conf
a2dissite default-ssl.conf 000-default.conf
service apache2 reload

# Laravel Configuration
mkdir /var/www/html/laravel
cp -r $DIR/* /var/www/html/laravel
cd /var/www/html/laravel/

chown -R www-data:www-data /var/www/html/laravel
chmod -R 775 /var/www/html/laravel
chmod -R 775 /var/www/html/laravel/storage
chmod -R 775 /var/www/html/laravel/bootstrap/cache

cat <<EOF > .env
APP_NAME=Laravel
APP_ENV=local
APP_KEY=
APP_DEBUG=true
APP_URL=http://admire

LOG_CHANNEL=stack
LOG_DEPRECATIONS_CHANNEL=null
LOG_LEVEL=debug

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=admire
DB_USERNAME=laravel
DB_PASSWORD=laravel

BROADCAST_DRIVER=log
CACHE_DRIVER=file
FILESYSTEM_DRIVER=local
QUEUE_CONNECTION=database
SESSION_DRIVER=database
SESSION_LIFETIME=120

MEMCACHED_HOST=127.0.0.1

REDIS_HOST=127.0.0.1
REDIS_PASSWORD=null
REDIS_PORT=6379

MAIL_MAILER=smtp
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=null
MAIL_PASSWORD=null
MAIL_ENCRYPTION=null
MAIL_FROM_ADDRESS=null
MAIL_FROM_NAME="${APP_NAME}"

AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_DEFAULT_REGION=us-east-1
AWS_BUCKET=
AWS_USE_PATH_STYLE_ENDPOINT=false

PUSHER_APP_ID=
PUSHER_APP_KEY=
PUSHER_APP_SECRET=
PUSHER_APP_CLUSTER=mt1

MIX_PUSHER_APP_KEY="${PUSHER_APP_KEY}"
MIX_PUSHER_APP_CLUSTER="${PUSHER_APP_CLUSTER}"
EOF

composer install
npm install
npm clean-install
php artisan key:generate
php artisan migrate:fresh --seed
npm run watch

