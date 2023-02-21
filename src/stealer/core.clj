(ns stealer.core 
  (:gen-class)
  (:require
    [stealer.lambda    :as lambda]
    [stealer.polling   :as polling]
    
    [clojure.string :as str]
    [clojure.edn :as edn]
    
    [dynamodb.api :as api]))

(defn lambda
  [config]
  (-> (lambda/->request config)
      (lambda/handle-request! config)
      (lambda/response->)))


(defn -main

  ([run-fn
    my-token
    db-creds
    & instances]
  (let [{:keys [access-key secret-key endpoint region table]} 
        (edn/read-string db-creds)]
    (polling/run-polling
             {:token my-token
              :instances (as-> instances insts
                           (map edn/read-string insts)
                           (map (fn [{:keys [caption-url] :as i}] 
                                  (assoc i :caption (some? (seq caption-url)))) insts))
              :client
              (api/make-client
                  access-key
                  secret-key
                  endpoint
                  region)
              :table table}))))


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
  
  (-main
    "...:..."
    "{:access-key \" \" 
      :secret-key \" \" 
      :endpoint \" \" 
      :region \"ru-central1\"
      :table \"stealer\"}"
    "{:chat-id 12345 
      :admin-chat-id 12346 
      :debug-chat-id nil 
      :trigger-id \"sjdfkjsdflkjslkdjh\" 
      :caption-url \"https://vk.com\"}")
  
  
  
  
  (polling/run-polling
    {:token "..."
     :db-creds "..."
     :chat-id -1001821581750
     })
  
  
  
  
  
  
  
  )