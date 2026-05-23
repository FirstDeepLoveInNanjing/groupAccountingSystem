-- 团体活动记账平台数据库初始化脚本
-- 仅包含业务表，不包含MySQL系统视图

CREATE DATABASE IF NOT EXISTS group_accounting DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE group_accounting;

-- 用户表
CREATE TABLE IF NOT EXISTS ordinary_user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    user_password VARCHAR(255) NOT NULL,
    user_name VARCHAR(50),
    user_mailbox VARCHAR(100),
    real_name VARCHAR(50),
    gender VARCHAR(10),
    birthday DATE,
    invite_code VARCHAR(20),
    payment_qr_code_path VARCHAR(255),
    total_income DECIMAL(10,2) DEFAULT 0,
    total_expense DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员表
CREATE TABLE IF NOT EXISTS administrator (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    admin_name VARCHAR(50) NOT NULL,
    admin_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 活动表
CREATE TABLE IF NOT EXISTS group_activity (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_name VARCHAR(100) NOT NULL,
    activity_description TEXT,
    actual_amount DECIMAL(10,2) DEFAULT 0,
    creator_id INT NOT NULL,
    is_settable BOOLEAN DEFAULT FALSE,
    confirm_initiated BOOLEAN DEFAULT FALSE,
    settle_unavailable_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES ordinary_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 活动成员表
CREATE TABLE IF NOT EXISTS activity_member (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(20) DEFAULT 'member',
    confirm_attend_status VARCHAR(20) DEFAULT 'unconfirmed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES group_activity(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES ordinary_user(user_id),
    UNIQUE KEY unique_activity_user (activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收款表
CREATE TABLE IF NOT EXISTS collect (
    collect_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    collect_status VARCHAR(20) DEFAULT 'pending',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES group_activity(activity_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 付款表
CREATE TABLE IF NOT EXISTS pay (
    pay_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    user_id INT NOT NULL,
    pay_amount DECIMAL(10,2) NOT NULL,
    pay_status VARCHAR(20) DEFAULT 'unpaid',
    pay_method VARCHAR(50),
    pay_remark TEXT,
    payment_proof_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES group_activity(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES ordinary_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 举报表
CREATE TABLE IF NOT EXISTS report (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    reporter_id INT NOT NULL,
    accused_activity_id INT NOT NULL,
    accused_user_id INT,
    report_reason TEXT NOT NULL,
    report_process_status VARCHAR(20) DEFAULT 'pending_review',
    punishment_type VARCHAR(50),
    punishment_end_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES ordinary_user(user_id),
    FOREIGN KEY (accused_activity_id) REFERENCES group_activity(activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员
INSERT IGNORE INTO administrator (admin_id, admin_name, admin_password) VALUES 
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO');

-- 插入测试用户
INSERT IGNORE INTO ordinary_user (user_id, phone_number, user_password, user_name, real_name) VALUES 
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '管理员', '系统管理员'),
(2, '13812345678', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '测试用户', '张三');
