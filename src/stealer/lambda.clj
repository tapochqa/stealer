;; https://github.com/ring-clojure/ring/blob/master/SPEC
;; https://cloud.yandex.ru/docs/functions/concepts/function-invoke

(ns stealer.lambda
  
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [clojure.string :as str]
   [stealer.handling :as handling])
  
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
           headers]}]
  {:remote-addr (get-in requestContext ["identity" "sourceIp"])
   :uri (if (= path "") "/" path)
   :query-params queryStringParameters
   :request-method (-> httpMethod name str/lower-case keyword)
   :headers (update-keys headers str/lower-case)
   :body (if isBase64Encoded
           (-> body
               (str->bytes "UTF-8")
               (b64-decode)
               (io/input-stream))
           (-> body
               (str->bytes "UTF-8")
               (io/input-stream)))})

(comment
  

  
  (-> (parse-request (json/parse-string (slurp "resources/yc-request.json")))
    :body
    slurp
    (json/parse-string true))
  
  )



(defn ->request []
  (-> *in*
      (json/parse-stream)
      (parse-request)))


(defn encode-body [body]
  (cond

    (string? body)
    {:body body
     :isBase64Encoded false}

    :else
    (throw (ex-info "Wrong response body"
                    {:body body}))))


(defn handle-request!
  [{:keys [headers body] :as request} config]
  
  (let [message (-> body
                 slurp
                 (json/parse-string true)
                 :message)]
  
  
  {:body
   (json/encode 
     (handling/the-handler
       message
       config))
   
   :headers headers
   
   :status 200}))


(defn response->
  [{:keys [status headers body]}]
  (json/with-writer [*out* nil]
    (json/write
     (cond-> nil
       status
       (assoc :statusCode status)
       headers
       (assoc :headers headers)))))
