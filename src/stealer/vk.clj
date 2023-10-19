(ns stealer.vk
  (:require
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as spec]))


(defn filter-params
  "
  Filter out nil values from a map.
  "
  [params]
  (persistent!
   (reduce-kv
    (fn [result k v]
      (if (some? v)
        (assoc! result k v)
        result))
    (transient {})
    params)))


(defn encode-params
  "
  JSON-encode complex values of a map.
  "
  [params]
  (persistent!
   (reduce-kv
    (fn [result k v]
      (if (coll? v)
        (assoc! result k (json/generate-string v))
        (assoc! result k v)))
    (transient {})
    params)))


(defn api-request
  [{:keys [vk-token
           user-agent
           timeout
           keepalive
           test-server
           local-server]}

   api-method http-method params]

  (let [params
        (filter-params params)
        
        params
        (conj params
          {:access_token vk-token
           :v "5.154"})

        url
        (format "https://api.vk.com/method/%s" 
           (name api-method))

        request
        {:url url

         :method http-method

         :as :stream}

        request
        (cond-> request

          user-agent
          (assoc :user-agent user-agent)

          timeout
          (assoc :timeout timeout)

          keepalive
          (assoc :keepalive keepalive))

        request
        (cond-> request

          ;; for GET, complex values must be JSON-encoded
          (= :get http-method)
          (assoc :query-params (encode-params params))

          (= :post http-method)
          (->
           (assoc-in [:headers "content-type"] "application/json")
           (assoc :body (json/generate-string params)))

          (= :post-multipart http-method)
          (->
            (assoc-in [:headers "content-type"] "multipart/form-data")
            (assoc :method :post)
            (assoc :multipart

              (let [params (map filter-params (map second params))
                    ;params (filter-params params)
                    ]
                (into
                  (for [[key value] (first params)]
                      (try
                        {:name (name key)
                         :content (json/generate-string value)}
                        (catch Exception e
                          {:name (name key)
                           :content value
                           :filename (clojure.string/replace (str value) #"/" "")
                           })))
                  (for [[key value] (second params)]
                      {:name (name key)
                       :content (str value)}))))))

        {:keys [error status body headers]}
        @(http/request request)]

        (if error
          (throw (ex-info (format "VK HTTP error: %s" (ex-message error))
                          {:api-method api-method
                           :api-params params}
                          error))

          (let [{:keys [content-type]}
                headers

                json?
                (some-> content-type
                        (str/starts-with? "application/json"))

                ;; parse JSON manually as Http Kit cannot
                body-json
                (if json?
                  (-> body io/reader (json/decode-stream keyword))
                  (throw (ex-info (format "VK response was not JSON: %s" content-type)
                                  {:http-status status
                                   :http-method http-method
                                   :http-headers headers
                                   :api-method api-method
                                   :api-params params})))

                {:keys [response
                        error]
                 :as fin}
                body-json]

            (if response
              response
              (throw (ex-info (format "VK API error: %s %s %s"
                                      (:error_code error) 
                                      api-method 
                                      (:error_msg error))
                              {:http-status status
                               :http-method http-method
                               :api-method api-method
                               :api-params params
                               :error-code (:error_code error)
                               :error (:error_msg error)})))))))


(defn groups--get-by-id
  "https://vk.com/dev/groups.getById"
  ([config]
   (api-request
     config
     :groups.getById
     :get
     {}))
  ([config {:keys [group_id 
                   group_ids 
                   fields]
            :as params}]
   (api-request
     config
     :groups.getById
     :get
     params)))


(defn groups--get-callback-confirmation-code
  "https://vk.com/dev/groups.getCallbackConfirmationCode"
  ([config]
   (api-request
     config
     :groups.getCallbackConfirmationCode
     :get
     {:group_id
      (-> (groups--get-by-id config)
          :groups
          first
          :id)}))
  ([config {:keys [group_id]
            :as params}]
    (api-request 
      config
      :groups.getCallbackConfirmationCode
      :get
      params)))




(comment
  (def ML {:vk-token (slurp "token-ml")})
  
  (groups--get-callback-confirmation-code ML)
  (groups--get-by-id ML)
  )