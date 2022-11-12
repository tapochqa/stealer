(ns stealer.sql
  (:require 
    [next.jdbc :as jdbc]
    [clojure.string :as str]))


(defn jdbc-mysql
  "user@host:port password"
  [expr]

  (let [expr (str/split expr #" ")
        pass (last expr)
        rest (str/split (first expr) #"[@:]")]  
    
    (jdbc/get-datasource
      {:dbtype "mysql"
       :host (nth rest 1)
       :port (nth rest 2)
       :user (nth rest 0)
       :password pass})))

(defn insert-row!
  [ds {:keys [bot-id chat-id from-chat-id message-id]}]


  (jdbc/execute! ds 
           ["INSERT INTO `telegram`.`stealer`
             (`bot_id`,
              `chat_id`,
              `from_chat_id`,
              `message_id`)
             VALUES (?, ?, ?, ?)
            "
            
            bot-id
            chat-id 
            from-chat-id
            message-id]))

(defn get-first-message
  "#:stealer{:id 6, :bot_id 1, :chat_id 1, :from_chat_id 1, :message_id 1}"
  [ds bot-id]
  (jdbc/execute-one! ds
    ["SELECT * FROM `telegram`.`stealer`
      WHERE `bot_id` = ?
      ORDER BY `id` ASC LIMIT 1" bot-id]))


(defn delete-row!
  [ds id]
  (jdbc/execute! ds
    ["DELETE FROM `telegram`.`stealer` WHERE (`id` = ?)" id]))


(comment 
  
  (jdbc-mysql "git@example.ru:3306 password")
  (insert-row! ds {:bot-id 1 
                   :chat-id 1 
                   :from-chat-id 1 
                   :message-id 1})
  (get-first-message ds 1)
  
  (:stealer/id #:stealer{:id 6, :bot_id 1, :chat_id 1, :from_chat_id 1, :message_id 1})
  ())



