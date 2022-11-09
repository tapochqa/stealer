lint:
	clj-kondo --lint src

NAME = stealer

NI_TAG = ghcr.io/graalvm/native-image:22.2.0

NI_ARGS = \
	--initialize-at-build-time \
	--report-unsupported-elements-at-runtime \
	--no-fallback \
	-jar ${JAR} \
	-J-Dfile.encoding=UTF-8 \
	--enable-http \
	--enable-https \
	-H:+PrintClassInitialization \
	-H:+ReportExceptionStackTraces \
	-H:Log=registerResource \
	-H:Name=./builds/${NAME}-

PLATFORM = PLATFORM

JAR = target/uberjar/${NAME}.jar

DATE = $(shell date +%s)

set-webhook:
	curl \
	--request POST \
	--url 'https://api.telegram.org/bot$(token)/setWebhook' \
	--header 'content-type: application/json' \
	--data '{"url": "https://functions.yandexcloud.net/$(id)}'

platform-docker:
	docker run -it --rm --entrypoint /bin/sh ${NI_TAG} -c 'echo `uname -s`-`uname -m`' > ${PLATFORM}

build-binary-docker: uberjar platform-docker
	docker run -it --rm -v ${PWD}:/build -w /build ${NI_TAG} ${NI_ARGS}$(shell cat ${PLATFORM})

platform-local:
	echo `uname -s`-`uname -m` > ${PLATFORM}

graal-build: platform-local
	native-image ${NI_ARGS}$(shell cat ${PLATFORM})

build-binary-local: uberjar graal-build


uberjar:
	lein uberjar

zip:
	zip -j target/${NAME}-${DATE}.zip conf/handler.sh builds/${NAME}-Linux-x86_64

bash-package: build-binary-docker zip



