SHELL:=zsh

all: bash-package

clean: lint

lint:
	clj-kondo --lint src

NAME = stealer

NI_TAG = ghcr.io/graalvm/native-image:22.2.0

NI_ARGS = \
	--initialize-at-build-time \
	--report-unsupported-elements-at-runtime \
	--no-fallback \
	--no-server \
	-jar ${JAR} \
	-J-Dfile.encoding=UTF-8 \
	--enable-url-protocols=http,https \
	-H:+PrintClassInitialization \
	-H:+ReportExceptionStackTraces \
	-H:Log=registerResource \
	--initialize-at-run-time=com.mysql.cj.jdbc.AbandonedConnectionCleanupThread \
	--initialize-at-run-time=com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.AbandonedConnectionCleanupThread \
	--initialize-at-run-time=com.mysql.cj.jdbc.Driver \
	--initialize-at-run-time=com.mysql.cj.jdbc.NonRegisteringDriver \
	-H:ReflectionConfigurationFiles=reflection-config.json \
	-H:Name=./builds/${NAME}-

PLATFORM = PLATFORM

JAR = target/uberjar/${NAME}.jar

DATE = $(shell date +%s)

VERSION = $(shell lein project-version)

HANDLER = conf/handler.sh

version:
	ghead -n -1 ${HANDLER} > temp ; mv temp ${HANDLER}
	echo \#\ Stealer\ ${VERSION} >> ${HANDLER}

set-webhook:
	curl 'https://api.telegram.org/bot$(token)/setWebhook?url=https://functions.yandexcloud.net/$(id)'

set-webhook-by-url:
	curl 'https://api.telegram.org/bot$(token)/setWebhook?url=$(url)'

delete-webhook:
	curl \
	--request POST \
	--url 'https://api.telegram.org/bot$(token)/deleteWebhook'

create-schema:
	mysql -h $(host) -u $(user) -p mysql < sql/create-schema.sql

create-table:
	mysql -h $(host) -u $(user) -p mysql < sql/create-table.sql

sql: create-schema create-table

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
	zip -j target/${NAME}.zip conf/handler.sh builds/${NAME}-Linux-x86_64

bash-package: version build-binary-docker zip

light-bash-package: build-binary-docker zip

package-with-bar:
	echo -n "white" | nc -4u -w0 localhost 1738
	make bash-package
	echo -n "black" | nc -4u -w0 localhost 1738
