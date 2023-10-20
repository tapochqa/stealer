(ns stealer.handling
  (:require
    [tg-bot-api.telegram :as telegram]
    [stealer.db :as db]
    [stealer.vk :as vk]
    
    [clojure.spec.alpha :as spec]
    [clojure.edn :as edn]
    [clojure.java.io :as io]))


(spec/def :confirmation/type #{"confirmation"})

(spec/def :confirmation/group_id int?)

(spec/def ::confirmation
  (spec/and 
    (spec/keys :req-un [:confirmation/type :confirmation/group_id])))


(spec/def ::message
  (spec/keys :req-un [:update/message]))


(spec/def ::callback-query
  (spec/keys :req-un [:update/callback_query]))




;;
;;
;;


(spec/def ::ne-string
  (spec/and string? not-empty))

(spec/def :size/height (spec/and int? (fn [x] (> x 0))))

(spec/def :size/width (spec/and int? (fn [x] (> x 0))))

(spec/def :size/url
  (spec/and ::ne-string
    (partial re-matches #"(?i)^http(s?)://.*")))

(spec/def ::size
  (spec/keys :req-un
    [:size/height
     :size/width
     :size/url]))

(spec/def :photo/sizes
  (spec/coll-of ::size :min-count 1))

(spec/def :attachments/photo
  (spec/keys :req-un 
    [:photo/sizes
     :photo/owner_id]))

(spec/def :attachment-photo/type #{"photo"})

(spec/def ::attachment-photo
  (spec/keys :req-un 
    [:attachment-photo/type
     :attachments/photo]))

(spec/def :message-w-photo/attachments
  (spec/coll-of ::attachment-photo :min-count 1 :max-count 1))

(spec/def :with-photo/message
  (spec/keys :req-un
          [:message-w-photo/attachments]))

(spec/def :with-photo/object
  (spec/keys :req-un
          [:with-photo/message]))

(spec/def :message/type #{"message_new"})

(spec/def ::vk-message-w-photo
    (spec/keys :req-un
          [:message/type
           :with-photo/object]))

(spec/valid? ::vk-message-w-photo
  (->
    (edn/read-string (slurp "msg-w-photo.edn"))
    :updates
    first))

;;
;;
;;


(def Length-command
  "Длина очереди")


(def Keyboard
     [[{:text Length-command}]])


(defn inline-keyboard
  [{message-id :message_id}]
  [[{:text "Удалить"
     :callback_data (- message-id)}
    {:text "Запостить"
     :callback_data message-id}]])


(defn start
  [config {{from-chat-id :id} :chat}]
  (telegram/send-message
    config
    from-chat-id
    (str "Сообщения принимаются только из админского чата. Айди этого чата: " from-chat-id)))


(defn ->tg
  [config instance message]
  (let [caption
        (:caption instance)
        
        caption-url
        (:caption-url instance)

        target-chat-id (:chat-id instance)
        admin-chat-id  (:admin-chat-id instance)
        
        title (->> target-chat-id (telegram/get-chat config) :title)

        payload
        (if caption
          {:caption title
           :caption-entities [{:type "text_link"
                               :url caption-url
                               :offset 0
                               :length (count title)}]}
          {:caption ""
           :caption-entities ""})]

    (telegram/copy-message
      config
      target-chat-id
      admin-chat-id
      (:message_id message)
      payload)))


(defn db->tg
  [{:keys [db-creds] :as config} admin-chat-id message-id]
  (let [res
        (if message-id
          (db/get-message config admin-chat-id message-id)
          (db/get-first-message config admin-chat-id))

        instance
        (first
          (filter
            (comp #{admin-chat-id} :admin-chat-id)
            (:instances config)))
        
        ]
    
    (->tg config instance res)
    res
    ))


(defn tg->db
  [{:keys [chat-id db-creds] :as config} message]

  (let [from-chat-id (-> message :chat :id)
        message-id (-> message :message_id)
        bot-id (:id (telegram/get-me config))]

    (db/insert-row!
      config
      message)

    (let
      [ update
        (telegram/send-message
          config
          from-chat-id
          (str "Чат `" from-chat-id "`\n"
               "Сообщение `" message-id "`")
          {:parse-mode "markdown"
           :reply-markup {:inline_keyboard (inline-keyboard message)}
           :reply-to-message-id message-id})

        bot-id (-> update :from :id)]

        (db/update-reply!
          config
          from-chat-id
          message-id
          update))))


(defn get-queue-length
  [{:keys [db-creds] :as config} from-chat-id]
  (telegram/send-message
    config
    from-chat-id
    (db/get-queue-length config from-chat-id)
    {:reply-markup {:keyboard Keyboard}}))


(defn delete-entry
  [config
   chat-id
   message-id
   status]

  (let [message (db/get-message config chat-id message-id)]

  (db/delete-message! config chat-id message-id)
  (telegram/edit-message-text
    config
    chat-id
    (get-in message [:reply :message_id])
    (str "`" status "`")
    {:parse-mode "markdown"})))


(defn get-instance
  [config message]
  (let [from-chat-id (get-in message [:chat :id])]
    (first
      (filter
        (comp #{from-chat-id} :admin-chat-id)
        (:instances config)))))


(defn the-router
        [config message]
        (let [from-chat-id      
              (get-in message [:chat :id])
              
              text              
              (:text message)

              instance
              (get-instance config message)]
          
          (cond
            (and
              (some? instance)
              (= text Length-command))
            (get-queue-length config from-chat-id)
            
            (some? instance)
            (if (some? (:trigger-id instance))
              (tg->db config message)
              (->tg config instance message))

            :else
            (start config message))))


(defn the-handler
  "Bot logic here"
  [config update trigger-id]
  
  (cond 

    (spec/valid? ::vk-message-w-photo update)
    (let [instance
          (first
            (filter
              (comp #{(-> update :object :message :peer_id)} :vk-admin-chat-id)
              (:instances config)))]
    
      (when
        (some? instance)
        (the-router
          config
          (telegram/send-photo
            config
            (:admin-chat-id instance)
            (io/input-stream
              (->> update :object :message :attachments first :photo :sizes
                          (sort-by :height)
                          reverse
                          first
                          :url)))))
      "ok")
    
    (spec/valid? ::confirmation update)
    (:code (vk/groups--get-callback-confirmation-code config))

    
    (spec/valid? ::message update)
    (let [message           (:message update)
          text              (:text message)
          from-chat-id      (get-in message [:chat :id])

          instance
          (get-instance config message)

          admin-chat-id     (:admin-chat-id instance)
          callback-query    (:callback_query update)
          bot-id            (:id (telegram/get-me config))
          from-id           (get-in message [:from :id])]

      
      (if (and message (not= bot-id from-id))
        (the-router config message))
      
      (if callback-query
        (let [callback-message  (:message callback-query)
              callback-data     (parse-long (:data callback-query))
              cb-message-id     (:message_id callback-message)
              admin-chat-id (get-in callback-message [:reply_to_message :chat :id])]

          (if (< callback-data 0)
            (delete-entry config admin-chat-id (- callback-data) "Удалено")
            (do
              (db->tg config admin-chat-id callback-data)
              (delete-entry config admin-chat-id callback-data "Запощено вручную")))))
      nil)

    (some? trigger-id)
    (let [instance
          (first
            (filter
              (comp #{trigger-id} :trigger-id)
              (:instances config)))

          admin-chat-id
          (:admin-chat-id instance)

          res
          (db->tg config admin-chat-id nil)

          message-id 
          (get-in res [:message :message_id])]
      (delete-entry config admin-chat-id message-id "Запощено по таймеру")
      nil)))


