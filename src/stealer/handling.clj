(ns stealer.handling
  (:require
    [stealer.telegram :as telegram]
    [stealer.sql :as sql]))


(defn db->tg
  [{:keys [db-creds] :as config}]
  (let [bot-id (:id (telegram/get-me config))
        ds (sql/jdbc-mysql db-creds)
        res (sql/get-first-message ds bot-id)]
    
    (telegram/copy-message
      config
      (:stealer/chat_id res)
      (:stealer/from_chat_id res)
      (:stealer/message_id res)
      {:caption ""
       :caption-entities ""})
    
    (sql/delete-row! ds (:stealer/id res))))


(defn tg->db
  [{:keys [token chat-id db-creds] :as config} message]
  
  (let [from-chat-id (-> message :chat :id)
        
        payload
        {:bot-id (:id (telegram/get-me config))
         :chat-id chat-id
         :from-chat-id from-chat-id
         :message-id (-> message :message_id)}]
    
    (sql/insert-row!
      (sql/jdbc-mysql db-creds)
      payload
      )
    
    (telegram/send-message 
      config 
      from-chat-id 
      (str "Бот `" (:bot-id payload) "`\n" 
           "Сообщение `" (:message-id payload) "`")
      {:parse-mode "markdown"})))

(defn the-handler 
  "Bot logic here"
  [config message trigger-id]
  
  (if message
    (tg->db config message))
  (if trigger-id
    (db->tg config)))


