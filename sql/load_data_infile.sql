CREATE TABLE load_data_table (
  id    INT,
  col00 VARCHAR(100) NOT NULL
);

#/usr/local/mysql/bin/mysql --local-infile -uroot -p123456 -h 127.0.0.1 -P 3306
LOAD DATA LOCAL INFILE '/Users/zcg/project/labs/mysqlproxy-zcg/sql/load_data.txt' INTO TABLE load_data_table CHARACTER SET utf8 fields terminated by',' enclosed by '"';
