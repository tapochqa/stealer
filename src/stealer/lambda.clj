;; https://github.com/ring-clojure/ring/blob/master/SPEC
;; https://cloud.yandex.ru/docs/functions/concepts/function-invoke

(ns stealer.lambda
  
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [clojure.string :as str]
   [stealer.handling :as handling]
   [stealer.telegram :as telegram])
  
  (:import
   java.io.File
   java.io.InputStream
   java.util.Base64
   ))


(defn str->bytes
  ^bytes [^String string ^String encoding]
  (.getBytes string encoding))


(defn b64-decode
  [^bytes encoded]
  (.decode (Base64/getDecoder) encoded))


(defn parse-request
  [{:strs [requestContext
           path
           queryStringParameters
           httpMethod
           body
           isBase64Encoded
           headers
           messages] :as request} {:keys [debug-chat-id] :as config}]
  (let [parsed
        {:remote-addr (get-in requestContext ["identity" "sourceIp"])
         :uri (if (= path "") "/" path)
         :query-params queryStringParameters
         :request-method 
         (if httpMethod
           (-> httpMethod name str/lower-case keyword)
           :trigger)
         :headers (update-keys headers str/lower-case)
         :messages (json/generate-string messages)
         :body (if isBase64Encoded
                 (-> (str "" body)
                     (str->bytes "UTF-8")
                     (b64-decode)
                     (io/input-stream))
                 (-> (str "" body)
                     (str->bytes "UTF-8")
                     (io/input-stream)))}]
    (if debug-chat-id
      (telegram/send-message config debug-chat-id (str parsed)))
    parsed))


(defn ->request [config]
  (-> *in*
      (json/parse-stream)
      (parse-request config)))


(defn encode-body [body]
  (cond

    (string? body)
    {:body body
     :isBase64Encoded false}

    :else
    (throw (ex-info "Wrong response body"
                    {:body body}))))


(defn handle-request!
  [{:keys [headers body messages] :as request} config]
  
  (let [message (-> body
                 slurp
                 (json/parse-string true)
                 :message)
        trigger-id
                (-> messages
                  (json/parse-string true)
                  first
                  :details
                  :trigger_id 
                  )]
  
  {:body
   (json/encode 
     (handling/the-handler
       config
       message
       trigger-id))
   
   :headers headers
   
   :status 200}))


(defn response->
  [{:keys [status headers]}]
  (json/with-writer [*out* nil]
    (json/write
     (cond-> nil
       status
       (assoc :statusCode status)
       headers
       (assoc :headers headers)))))
