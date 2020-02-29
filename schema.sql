 CREATE TABLE `tbl_video` (
 `id` int(11) NOT NULL AUTO_INCREMENT,
 `url` text NOT NULL,
 `stream_url` VARCHAR(128),
 `title` VARCHAR(512) NOT NULL,
 `descr` VARCHAR(4096) NOT NULL,
 `tags` VARCHAR(128),
 `thumb` VARCHAR(128),
 `breadcrumbs` VARCHAR(64),
 `quality` SMALLINT,
 `account` VARCHAR(64),
 `video_id` VARCHAR(32),
 `date` datetime DEFAULT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `url` (`url`(70))
 ) ENGINE=InnoDB AUTO_INCREMENT=1319 DEFAULT CHARSET=utf8;


