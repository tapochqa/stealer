(ns stealer.core 
  (:gen-class)
  (:require
    [stealer.lambda    :as lambda]
    [clojure.string    :as str]
    [cheshire.core     :as json]))


(defn lambda
  [config]
  (-> (lambda/->request config)
      (lambda/handle-request! config)
      (lambda/response->)))


(defn -main
  [my-token chat-id debug-chat-id db-creds]
  (lambda  {:token my-token
            :db-creds db-creds
            :chat-id (parse-long chat-id)
            :debug-chat-id (parse-long debug-chat-id)}))


(comment
  
  (binding [*in* (-> "yc-request.json"
                   clojure.java.io/resource
                   clojure.java.io/reader)]
  
    (-main nil nil nil nil)
    
    )
  
  
  )