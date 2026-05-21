ALTER TABLE `user`
    ADD COLUMN `inviteCode` varchar(16) NULL COMMENT '用户邀请码' AFTER `paymentQrCodePath`;

UPDATE `user`
SET inviteCode = SUBSTRING(UPPER(MD5(CONCAT(userID, UUID()))), 1, 8)
WHERE userType = '普通用户'
  AND (inviteCode IS NULL OR inviteCode = '');

ALTER TABLE `user`
    MODIFY COLUMN `inviteCode` varchar(16) NULL COMMENT '用户邀请码',
    ADD UNIQUE INDEX `uk_user_invite_code` (`inviteCode`);

ALTER TABLE `groupactivity`
    ADD COLUMN `confirmInitiated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已由创建者发起确认' AFTER `isSettable`;

UPDATE `groupactivity`
SET budget = actualAmount
WHERE actualAmount IS NOT NULL;
