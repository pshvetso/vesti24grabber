# vesti24grabber<br>

###Install

sudo apt install -y default-jre mysql-server ffmpeg  
or download, unpzip and add ffmpeg binary files to Path variable

Create database  
CREATE DATABASE russia24-tv DEFAULT CHARACTER SET `utf8` COLLATE `utf8_general_ci`;  
Import data schema  
mysql -u root -p < schema.sql

Build jar artifact.  
Put vesti24grabber.jar and copyrighted.txt into /projects/vesti24grabber.  
Put video advertisement files into /projects/vesti24grabber/video.

edit logging.properties (/usr/lib/jvm/java-8-oracle/jre/lib/ или /etc/java-7-openjdk)  
handlers = java.util.logging.FileHandler, java.util.logging.ConsoleHandler  
java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter


###Настройка аккаунта Youtube в Google API Console  
Implementing OAuth 2.0 Authorization https://developers.google.com/youtube/v3/guides/authentication  
Using OAuth 2.0 for Web Server Applications https://developers.google.com/youtube/v3/guides/auth/server-side-web-apps  

Go to developers console https://console.developers.google.com/apis/library  
Выберите проект - Создать проект: RussiaToday  
Найдите YouTube Data API v3 и включите его.  
Go to the Credentials page https://console.developers.google.com/apis/credentials  
Создайте окно доступа:  
User Type - внешний  
Название приложения: Russia Today  
Сохранить  
Click Create credentials > OAuth client ID.  
Создать учётные данные - Идентификатор клиента OAuth  
Выберите Веб-приложение, введите название: Russia Today.  
Разрешенные источники JavaScript  
http://localhost:8080  
Разрешенные URI перенаправления  
http://localhost:8080/Callback  
Нажмите Создать  

https://vk.com/dev/first_guide  
https://vk.com/dev/publications  
https://vk.com/dev/permissions  
https://vk.com/dev/wall.post  
https://toster.ru/q/205566  
http://worldjb.ru/forum/threads/%D0%9F%D1%80%D0%B8%D0%BC%D0%B5%D1%80-%D0%BF%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F-%D0%B4%D0%BB%D1%8F-%D0%92%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%B5-%D0%BD%D0%B0-java.6973/  

