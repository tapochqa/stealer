#! /bin/bash

./stealer-Linux-x86_64 \
'...:...' \
'{:access-key    "..."
  :secret-key    "...-..."
  :endpoint      "https://docapi.serverless.yandexcloud.net/ru-central1/.../..."
  :region        "ru-central1"
  :table         "stealer"}' \
'{:chat-id       -...
  :admin-chat-id -... 
  :debug-chat-id nil
  :trigger-id    "..." 
  :caption-url   "https://..."}'

# https://stackoverflow.com/a/39943226/10354619
# Stealer 0.5.2
