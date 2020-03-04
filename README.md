# vesti24grabber

Install

sudo apt install -y default-jre mysql-server
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
