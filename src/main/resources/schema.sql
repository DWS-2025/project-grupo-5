-- Script to initialize the MySQL database

CREATE DATABASE IF NOT EXISTS musicstore;
USE musicstore;

-- Character set and collation configuration
ALTER DATABASE musicstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;