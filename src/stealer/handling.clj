(ns stealer.handling
  (:require
    [stealer.telegram :as telegram]
    [stealer.sql :as sql]))


(def Length-command
  "Длина очереди")


(def Keyboard
     [[{:text Length-command}]])


(defn inline-keyboard
  [{message-id :message_id}]
  [[{:text "Удалить"
     :callback_data (- message-id)}
    {:text "Запостить"
     :callback_data message-id}]])


(defn start
  [config {{from-chat-id :id} :from}]
  (telegram/send-message
    config
    from-chat-id
    "Сообщения принимаются только из админского чата."
    {:reply-markup {:keyboard Keyboard}}))


(defn db->tg
  [{:keys [db-creds caption caption-url] :as config} message-id]
  (let [bot-id (:id (telegram/get-me config))
        ds (sql/jdbc-mysql db-creds)
        
        res 
        (if message-id
          (sql/get-message ds bot-id message-id)
          (sql/get-first-message ds bot-id))
        
        
        chat-id (:stealer/chat_id res)
        title (->> chat-id (telegram/get-chat config) :title)
        
        payload
        (if caption
          {:caption title
           :caption-entities [{:type "text_link" 
                               :url caption-url 
                               :offset 0 
                               :length (count title)}]}
          {:caption ""
           :caption-entities ""})]
    
    (telegram/copy-message
      config
      chat-id
      (:stealer/from_chat_id res)
      (:stealer/message_id res)
      payload)
    res
    ))


(defn tg->db
  [{:keys [chat-id db-creds] :as config} message]
  
  (let [from-chat-id (-> message :chat :id)
        message-id (-> message :message_id)
        bot-id (:id (telegram/get-me config))
        
        payload
        {:bot-id bot-id
         :chat-id chat-id
         :from-chat-id from-chat-id
         :message-id message-id}]
    
    (sql/insert-row!
      (sql/jdbc-mysql db-creds)
      payload
      )
    
    (let 
      [ update
      
        (telegram/send-message 
          config 
          from-chat-id 
          (str "Бот `" (:bot-id payload) "`\n" 
               "Сообщение `" (:message-id payload) "`")
          {:parse-mode "markdown"
           :reply-markup {:inline_keyboard (inline-keyboard message)}
           :reply-to-message-id message-id})
       
        bot-id (-> update :from :id)]
      
        (sql/update-reply-id! 
            (sql/jdbc-mysql (:db-creds config)) 
          (:message_id update) bot-id message-id)
      )))


(defn get-queue-length
  [{:keys [db-creds] :as config} {{from-chat-id :id} :from}]
  (telegram/send-message
    config
    from-chat-id
    (sql/get-queue-length (sql/jdbc-mysql db-creds) (:id (telegram/get-me config)))
    {:reply-markup {:keyboard Keyboard}}))


(defn delete-entry
  [config 
   cb-message-id
   message-id
   status
   ]
  
  (let [ds (sql/jdbc-mysql (:db-creds config))
        bot-id (:id (telegram/get-me config))
        message (sql/get-message ds bot-id message-id)]
  
  (sql/delete-message! ds bot-id message-id)
  (telegram/edit-message-text 
    config 
    (:stealer/from_chat_id message) 
    cb-message-id
    (str "`" status "`")
    {:parse-mode "markdown"}
    )))


(defn the-handler 
  "Bot logic here"
  [config update trigger-id]
  
  (let [message           (:message update)
        text              (:text message)
        admin-chat-id     (:admin-chat-id config)
        from-chat-id      (get-in message [:chat :id])
        callback-query    (:callback_query update)
        bot-id            (:id (telegram/get-me config))
        from-id           (get-in message [:from :id])
        ]

    
    (if (and message (not= bot-id from-id))
      (cond
        
        (and 
          (= from-chat-id admin-chat-id)
          (= text Length-command))
        (get-queue-length config message)
        
        (= from-chat-id admin-chat-id)
        (tg->db config message)
        
        :else
        (start config message)
        ))
    
    (if callback-query
      (let [callback-message  (:message callback-query)
            callback-data     (parse-long (:data callback-query))
            cb-message-id     (:message_id callback-message)]
        
        (if (< callback-data 0)
          (delete-entry config cb-message-id (- callback-data) "Удалено")
          (do
            (db->tg config callback-data)
            (delete-entry config cb-message-id callback-data "Запощено вручную"))))))
  
  (if trigger-id
    (let [res (db->tg config nil)
          cb-message-id (:stealer/reply_id res)
          message-id (:stealer/message_id res)]
      (delete-entry config cb-message-id message-id "Запощено по таймеру"))))


