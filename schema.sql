-- phpMyAdmin SQL Dump
-- version 4.7.9
-- https://www.phpmyadmin.net/
--
-- Хост: 127.0.0.1
-- Время создания: Мар 01 2020 г., 15:42
-- Версия сервера: 10.1.31-MariaDB
-- Версия PHP: 5.6.34

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- База данных: `russia24-tv`
--

-- --------------------------------------------------------

--
-- Структура таблицы `ads`
--

CREATE TABLE `ads` (
  `id` int(11) NOT NULL,
  `text` varchar(255) NOT NULL,
  `video` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `ads`
--

INSERT INTO `ads` (`id`, `text`, `video`) VALUES
(1, 'Купить видеокарту XFX RX 570 4 Гб https://s.click.aliexpress.com/e/_dVxwbwd', 'screencast-aliexpress.ru-Купить видеокарту XFX RX 570 4 Гб.mp4'),
(2, 'Видеокарта Gigabyte GTX 1050 Ti CN 4 ГБ с доставкой https://s.click.aliexpress.com/e/_etIztX', 'screencast-aliexpress.ru-Видеокарта Gigabyte GTX 1050 Ti CN.mp4'),
(3, 'Купить корпус Deepcool Matrexx 55 Black https://s.click.aliexpress.com/e/_eMXKoh', 'screencast-aliexpress.ru-Чехол для компьютера DEEPCOOL MATREXX55.mp4'),
(4, 'Смартфон Xiaomi Redmi Note 7 6/64 Гбайт https://s.click.aliexpress.com/e/_eMmpzJ', 'screencast-aliexpress.ru-https_aliexpress.ru_item_32966964008.html.mp4'),
(5, 'Смартфон Samsung Galaxy A50s (SM-A5070) LTE с доставкой https://s.click.aliexpress.com/e/_eLGaqd', 'screencast-aliexpress.ru-https_aliexpress.ru_item_4000324739140.html.mp4'),
(6, 'Смартфон Apple iPhone Xr 128 ГБ с доставкой https://s.click.aliexpress.com/e/_eN2vrb', 'screencast-tmall.ru-https_tmall.ru_item_Apple-iPhone-Xr-128_32948637890.html_productId=32948637890.mp4'),
(7, 'Смартфон Xiaomi Redmi 6A Octa Core 2/32 ГБ с доставкой https://s.click.aliexpress.com/e/_eNfEDL\r\n', 'screencast-aliexpress.ru-Смартфон Xiaomi Redmi 6A.mp4'),
(8, 'Смартфон Honor 8X с доставкой https://s.click.aliexpress.com/e/_eOWWDF\r\n', 'screencast-aliexpress.ru-Honor 8X.mp4'),
(9, 'Ноутбук Apple MacBook Air 13\": 1.8 ГГц Dual-Core Intel Core i5, 128 ГБ с доставкой https://s.click.aliexpress.com/e/_eMH9Jb\r\n', 'screencast-tmall.ru-Ноутбук Apple MacBook Air 13.mp4'),
(10, 'Ноутбук Xiaomi Mi Air Pro 15,6” GTX 1050 Max-Q  Intel Core i7 с доставкой https://s.click.aliexpress.com/e/_ettQcD\r\n', 'screencast-aliexpress.ru-Ноутбук Xiaomi Mi Air Pro 15,6.mp4'),
(11, 'Смарт-часы Apple Watch Series 5 GPS, 44 мм Aluminium Case, Sport Band с доставкой https://s.click.aliexpress.com/e/_eM0Rbr\r\n', 'screencast-tmall.ru-Смарт-часы Apple Watch Series 5 GPS.mp4'),
(12, 'Умные часы HUAWEI GT 2 GT2 Smart https://s.click.aliexpress.com/e/_ePrYJJ\r\n', 'screencast-aliexpress.ru-HUAWEI GT 2 GT2 Smart.webm'),
(13, 'Фитнес-браслет Huawei Honor Band 4 https://s.click.aliexpress.com/e/_eOz157\r\n', 'screencast-aliexpress.ru-Оригинальный Смарт браслет Huawei Honor Band 3_4.mp4'),
(14, 'Фитнес-трекер JET Sport FT-5C с доставкой https://s.click.aliexpress.com/e/_eLChbf\r\n', 'screencast-tmall.ru-Фитнес-трекер JET Sport FT-5C.mp4'),
(15, 'Наушники Apple AirPods 2 с доставкой https://s.click.aliexpress.com/e/_etpq8l\r\n', 'screencast-tmall.ru-Наушники Apple AirPods 2.mp4'),
(16, 'Электроскутер Kugoo M4 Pro оригинал 500 Вт со склада в России https://s.click.aliexpress.com/e/_eNWqYN\r\n', 'screencast-aliexpress.ru-KUGOO M4 PRO.mp4'),
(17, 'Новый оригинальный электроскутер Xiaomi Mijia M365 Pro Mi для взрослых https://s.click.aliexpress.com/e/_ePE1FJ\r\n', 'screencast-aliexpress.ru-Xiaomi mijia M365_Pro mi.mp4');

-- --------------------------------------------------------

--
-- Структура таблицы `tbl_video`
--

CREATE TABLE `tbl_video` (
  `id` int(11) NOT NULL,
  `video_id` varchar(32) DEFAULT NULL,
  `url` varchar(128) NOT NULL,
  `stream_url` varchar(256) DEFAULT NULL,
  `title` varchar(256) NOT NULL,
  `descr` varchar(2048) NOT NULL,
  `tags` varchar(128) DEFAULT NULL,
  `thumb` varchar(128) DEFAULT NULL,
  `breadcrumbs` varchar(64) DEFAULT NULL,
  `quality` smallint(6) NOT NULL,
  `account` varchar(64) DEFAULT NULL,
  `date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Индексы сохранённых таблиц
--

--
-- Индексы таблицы `ads`
--
ALTER TABLE `ads`
  ADD PRIMARY KEY (`id`);

--
-- Индексы таблицы `tbl_video`
--
ALTER TABLE `tbl_video`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `url` (`url`(70));

--
-- AUTO_INCREMENT для сохранённых таблиц
--

--
-- AUTO_INCREMENT для таблицы `ads`
--
ALTER TABLE `ads`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT для таблицы `tbl_video`
--
ALTER TABLE `tbl_video`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1334;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
