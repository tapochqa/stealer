# Стилер

Телеграм-бот, ворующий контент с других каналов. Копирует сообщения в канал/чат, стирая подписи.

Компилируется Граалем, деплоится в Яндекс-функцию.

Логика в `src/stealer/handling.clj`.

Для сборки установите Лейнинген и Докер.

Собрать: 
```bash
docker pull ghcr.io/graalvm/native-image:22.2.0
make bash-package
```

После сборки сделайте бота, Яндекс-функцию и установите вебхук: 
```bash
make set-webhook token="<Токен бота>" id="<Айди Яндекс-функции>"
```

Загрузите архив в функцию через обжект сторедж. 
Впишите в `handler.sh` токен, айди чата/канала и надо ли удалять сообщение в диалоге с ботом.
```bash
#! /bin/bash

./stealer-Linux-x86_64 "5631323282:AfZq11L..." "-10084234..." "true"
``` 

Дайте боту все нужные права в Телеге. Сделайте функцию публичной.

Почти весь код придумал [Иван Гришаев](https://grishaev.me), а я тупо скопировал.