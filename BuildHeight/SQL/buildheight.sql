INSERT INTO `permission_definitions`
    (`permission_key`, `max_value`, `comment`,
     `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('cmd_buildheight', 1,
     'Build-height popup (also requires room build rights). 0 denied, 1 allowed',
     1, 1, 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE `permission_key` = `permission_key`;

INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('building.cmd_buildheight.min_height', '-40'),
    ('building.cmd_buildheight.max_height', '40')
ON DUPLICATE KEY UPDATE `key` = `key`;
