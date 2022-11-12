CREATE TABLE `telegram`.`stealer` 
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bot_id` bigint DEFAULT NULL,
  `chat_id` bigint DEFAULT NULL,
  `from_chat_id` bigint DEFAULT NULL,
  `message_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci