(ns stealer.polling
  (:require 
    [tg-bot-api.telegram :as telegram]
    
    [stealer.handling :as handling]
    [stealer.vk :as vk]
    
    [cheshire.core :as json]
    ))


(defn save-offset [offset-file offset]
  (spit offset-file (str offset)))


(defn load-offset [offset-file]
  (try
    (-> offset-file slurp Long/parseLong)
    (catch Throwable _
      nil)))

(defmacro with-safe-log
  "
  A macro to wrap API calls (prevent the whole program from crushing).
  "
  [& body]
  `(try
     ~@body
     (catch Throwable e#
       (println (ex-message e#)))))


(defn run-polling
  [config]

  (let [tg-offset-file "TELEGRAM_OFFSET"
        vk-offset-file "VK_OFFSET"

        tg-offset
        (load-offset tg-offset-file)
        
        vk-offset
        (load-offset vk-offset-file)
        ]

    (loop [tg-offset tg-offset
           vk-offset vk-offset]

      (let [tg-updates
            (with-safe-log
              (telegram/get-updates config {:offset tg-offset}))
            
            vk-updates
            (with-safe-log
              (vk/get-updates config {:ts vk-offset}))
            
            new-tg-offset
            (or (some-> tg-updates peek :update_id inc)
                tg-offset)
            
            new-vk-offset
            (or (some-> vk-updates :ts)
                vk-offset)
            ]

        (println "Telegram: got %s updates, next offset: %s, updates: %s"
            (count tg-updates)
            new-tg-offset
            (json/generate-string tg-updates {:pretty true}))
        
        (println "VK: got %s updates, next offset: %s, updates: %s"
            (count vk-updates)
            new-vk-offset
            (json/generate-string vk-updates {:pretty true}))

        (when tg-offset
          (save-offset tg-offset-file new-tg-offset))
        (when vk-offset
          (save-offset vk-offset-file new-vk-offset))
        (doseq [update (concat tg-updates (:updates vk-updates))]
          (handling/the-handler config update nil))

        (recur new-tg-offset
               new-vk-offset)))))