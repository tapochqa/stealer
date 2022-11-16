(ns stealer.core 
  (:gen-class)
  (:require
    [stealer.lambda    :as lambda]
    [stealer.polling   :as polling]))


(defn lambda
  [config]
  (-> (lambda/->request config)
      (lambda/handle-request! config)
      (lambda/response->)))


(defn -main
  [my-token
   chat-id
   debug-chat-id
   caption-url
   db-creds]
  (lambda  {:token my-token
            :db-creds db-creds
            :chat-id (parse-long chat-id)
            :debug-chat-id (parse-long debug-chat-id)
            :caption (some? (seq caption-url))
            :caption-url caption-url}))


(comment
  (-main "123" "-1231 -1232 -1233" "" "https://vk.com" "user@host:port password")
  
  (binding [*in* (-> "trigger-request.json"
                 clojure.java.io/resource
                 clojure.java.io/reader)]

    (-main 
      "..."
      "-1001821581750"
      "..."
      "http://vk.com"
      ""))
  
  
  
  
  (polling/run-polling
    {:token "..."
     :db-creds "..."
     :chat-id -1001821581750
     })
  
  
  
  
  
  
  
  )