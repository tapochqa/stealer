(ns stealer.handling
  (:require
    [tg-bot-api.telegram :as telegram]
    [stealer.db :as db]))


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


(defn db->tg
  [{:keys [db-creds caption caption-url] :as config} admin-chat-id message-id]
  (let [res
        (if message-id
          (db/get-message config admin-chat-id message-id)
          (db/get-first-message config admin-chat-id))

        instance
        (first
          (filter
            (comp #{admin-chat-id} :admin-chat-id)
            (:instances config)))

        target-chat-id (:chat-id instance)
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
      (:message_id res)
      payload)
    res))


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


(defn the-handler
  "Bot logic here"
  [config update trigger-id]

  (let [message           (:message update)
        text              (:text message)
        from-chat-id      (get-in message [:chat :id])

        instance
        (first
          (filter
            (comp #{from-chat-id} :admin-chat-id)
            (:instances config)))

        admin-chat-id     (:admin-chat-id instance)
        callback-query    (:callback_query update)
        bot-id            (:id (telegram/get-me config))
        from-id           (get-in message [:from :id])]

    (if (and message (not= bot-id from-id))
      (cond
        (and
          (some? instance)
          (= text Length-command))
        (get-queue-length config from-chat-id)
        
        (some? instance)
        (tg->db config message)

        :else
        (start config message)))
    (if callback-query
      (let [callback-message  (:message callback-query)
            callback-data     (parse-long (:data callback-query))
            cb-message-id     (:message_id callback-message)
            admin-chat-id (get-in callback-message [:reply_to_message :chat :id])]

        (if (< callback-data 0)
          (delete-entry config admin-chat-id (- callback-data) "Удалено")
          (do
            (db->tg config admin-chat-id callback-data)
            (delete-entry config admin-chat-id callback-data "Запощено вручную"))))))

  (if trigger-id
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
          (:message_id res)]
      (delete-entry config admin-chat-id message-id "Запощено по таймеру"))))


