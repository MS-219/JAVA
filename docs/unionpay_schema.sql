CREATE TABLE IF NOT EXISTS `up_member` (
  `member_code` varchar(32) NOT NULL,
  `member_unique_id` varchar(64) DEFAULT NULL,
  `member_name` varchar(64) DEFAULT NULL,
  `mobile_no` varchar(32) DEFAULT NULL,
  `cert_no` varchar(32) DEFAULT NULL,
  `depart_code` varchar(32) DEFAULT NULL,
  `depart_name` varchar(64) DEFAULT NULL,
  `member_type_name` varchar(64) DEFAULT NULL,
  `state_flag` int DEFAULT NULL,
  `deleted_flag` int DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `age` int DEFAULT NULL,
  `sex` int DEFAULT NULL,
  `nation` varchar(64) DEFAULT NULL,
  `household_address` varchar(255) DEFAULT NULL,
  `live_address` varchar(255) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `synced_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `raw_payload` longtext,
  PRIMARY KEY (`member_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `up_member_card` (
  `card_code` varchar(32) NOT NULL,
  `member_code` varchar(32) DEFAULT NULL,
  `card_no` varchar(64) DEFAULT NULL,
  `member_card_type` varchar(64) DEFAULT NULL,
  `balance` bigint DEFAULT NULL,
  `face_bind` int DEFAULT NULL,
  `loss_state` int DEFAULT NULL,
  `lock_state` int DEFAULT NULL,
  `enable_state` int DEFAULT NULL,
  `deleted_flag` int DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `synced_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `raw_payload` longtext,
  PRIMARY KEY (`card_code`),
  KEY `idx_card_member` (`member_code`),
  KEY `idx_card_no` (`card_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `up_member_face` (
  `card_no` varchar(64) NOT NULL,
  `face_code` varchar(64) DEFAULT NULL,
  `member_code` varchar(32) DEFAULT NULL,
  `binding_state` int DEFAULT NULL,
  `image_path` varchar(255) DEFAULT NULL,
  `image_hash` varchar(64) DEFAULT NULL,
  `synced_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `raw_payload` longtext,
  PRIMARY KEY (`card_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `up_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sync_type` varchar(64) NOT NULL,
  `page_no` int DEFAULT NULL,
  `page_size` int DEFAULT NULL,
  `success_flag` tinyint(1) DEFAULT NULL,
  `resp_code` varchar(16) DEFAULT NULL,
  `resp_desc` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

