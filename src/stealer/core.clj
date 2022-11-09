(ns stealer.core 
  (:gen-class)
  (:require
    [stealer.lambda    :as lambda]
    [clojure.string    :as str]
    [cheshire.core     :as json]))


(defn lambda
  [config]
  (-> (lambda/->request)
      (lambda/handle-request! config)
      (lambda/response->)))


(defn -main
  [my-token chat-id post-rm]
  (lambda  {:token my-token
            :chat-id (parse-long chat-id)
            :post-rm (parse-boolean post-rm)}))
