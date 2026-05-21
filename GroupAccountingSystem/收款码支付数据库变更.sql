-- 收款码付款与付款凭证上传数据库变更
-- 用途：改为创建者上传收款码，参与者扫码转账后上传付款记录图片。
-- 注意：执行前请先备份数据库；如果字段已存在，请跳过对应 ALTER TABLE。

ALTER TABLE user
    ADD COLUMN paymentQrCodePath varchar(255) NULL COMMENT '创建者收款码图片访问路径' AFTER Birthday;

ALTER TABLE pay
    ADD COLUMN paymentProofPath varchar(255) NULL COMMENT '参与者付款记录图片访问路径' AFTER notifyTime;
