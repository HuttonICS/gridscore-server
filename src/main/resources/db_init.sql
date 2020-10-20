DROP TABLE IF EXISTS `configurations`;
CREATE TABLE `configurations`  (
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `configuration` json NOT NULL,
  `created_on` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`uuid`) USING BTREE,
  UNIQUE INDEX `configuration_uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;