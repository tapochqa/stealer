(ns stealer.db
  (:require
    [dynamodb.api :as api]
    [dynamodb.constant :as const]))


(defn insert-row!
  [{:keys [client table]} message]
  (api/put-item
    client
    table
    {:chat_id (get-in message [:chat :id])
     :message_id (:message_id message)
     :message message}))


(defn get-first-message
  [{:keys [client table]} chat-id]
  (->
    (api/query 
      client 
      table
      {:sql-key "#id = :one"
       :attr-names {"#id" :chat_id}
       :attr-values {":one" chat-id}
       :limit 1})
    :Items
    first))


(defn get-message
  [{:keys [client table]} chat-id message-id]
  (:Item
    (api/get-item
      client
      table
      {:chat_id chat-id
       :message_id message-id})))


(defn get-queue-length
  [{:keys [client table]} chat-id]
  (:Count
    (api/query
      client 
      table
      {:sql-key "#id = :one"
       :attr-names {"#id" :chat_id}
       :attr-values {":one" chat-id}})))


(defn delete-message!
  [{:keys [client table]} chat-id message-id]
  (api/delete-item
    client
    table
    {:chat_id chat-id
     :message_id message-id}))


(defn update-reply!
  [{:keys [client table]} chat-id message-id reply]
  (api/update-item
    client
    table
    {:message_id message-id
     :chat_id chat-id}
    {:reply reply}))


(comment
  
  
  (def CLIENT
    (api/make-client "..."
                     "..."
                     "https://docapi.serverless.yandexcloud.net/ru-central1/.../..."
                     "ru-central1"))
  
  (api/create-table CLIENT
                  "stealer-3"
                  {:chat_id :N
                   :message_id :N}
                  {:chat_id const/key-type-hash
                   :message_id const/key-type-range}
                  {:table-class "DOCUMENT"})
  
  
  (api/put-item 
    CLIENT
    "stealer-3"
    {:chat_id 1234
     :message_id 1
     :message {:a "B" :c "D"}})
  
  (api/update-item
    CLIENT 
    "stealer-3"
    {:chat_id 1234
     :message_id 1}
    {:set {:reply {:foo "baz"}}})
  
  (get-first-message {:client CLIENT :table "stealer-3"} 1234)
  (get-queue-length {:client CLIENT :table "stealer-3"} 1234)
  (get-message {:client CLIENT :table "stealer-3"} 1234 1) 
  
  (api/query CLIENT
             "stealer-3"
             {:sql-key "#id = :one"
              :attr-names {"#id" :chat_id}
              :attr-values {":one" 1234}})
  
  
  
  
  
  
  
  
  
  
  
  
  
  )