DROP PROCEDURE IF EXISTS `prc_test`;

DELIMITER ;;
CREATE PROCEDURE `prc_test`()
  BEGIN
  END;
;;
DELIMITER ;


call prc_test();


DROP PROCEDURE IF EXISTS `prc_test01`;

DELIMITER ;;
CREATE PROCEDURE `prc_test01`()
  BEGIN
    select 1;
  END;
;;
DELIMITER ;


call prc_test01();


DROP PROCEDURE IF EXISTS `prc_test02`;

DELIMITER ;;
CREATE PROCEDURE `prc_test02`()
  BEGIN
    select 1;
    select 2;
  END;
;;
DELIMITER ;


call prc_test02();
