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
      (lambda/response-> config)))

(defn -main
  [tg-token
   vk-token
   db-creds
   & instances]
  (let [{:keys [access-key secret-key endpoint region table]} 
        (edn/read-string db-creds)
        
        config
        { :test-server false
          :token tg-token
          :vk-token vk-token
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
          :table table
          }]
      #_(polling/run-polling config)
      (lambda config)))

(comment
  
  (binding [*in* (-> "trigger-request.json"
                 clojure.java.io/resource
                 clojure.java.io/reader)]

    (-main
      "...:..."
      "..."
      "{:access-key \" \" 
        :secret-key \" \" 
        :endpoint \" \" 
        :region \"ru-central1\"
        :table \"stealer\"}"
      "{:chat-id 12345 
        :admin-chat-id 12346 
        :debug-chat-id nil 
        :trigger-id \"sjdfkjsdflkjslkdjh\" 
        :caption-url \"https://vk.com\"}")))