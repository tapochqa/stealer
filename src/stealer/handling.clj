(ns stealer.handling
  (:require
    [stealer.telegram :as telegram]))


(defn the-handler 
  "Bot logic here"
  [message {:keys [token chat-id post-rm] :as config}]
  
  (let [from-id    (-> message :chat :id)
        message-id (-> message :message_id)]
  
    (telegram/copy-message
      config
      chat-id
      from-id
      message-id
      {:caption ""
       :caption-entities ""})
    
    (when post-rm
      (telegram/delete-message
        config
        from-id
        message-id))))
