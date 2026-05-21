ALTER TABLE report
    ADD COLUMN accusedUserID INT NULL COMMENT '被举报用户ID，外键关联User(userID)' AFTER accusedActivityID,
    ADD COLUMN punishmentType VARCHAR(20) NULL COMMENT '处罚类型：警告/封号1天/封号7天/封号365天' AFTER reportProcessStatus,
    ADD COLUMN punishmentEndTime DATETIME NULL COMMENT '封号截止时间' AFTER punishmentType;

UPDATE report
SET accusedUserID = reporterID
WHERE accusedUserID IS NULL;

ALTER TABLE report
    MODIFY accusedUserID INT NOT NULL,
    ADD INDEX accusedUserID (accusedUserID),
    ADD CONSTRAINT report_ibfk_3 FOREIGN KEY (accusedUserID) REFERENCES user (userID)
        ON DELETE CASCADE ON UPDATE RESTRICT;
