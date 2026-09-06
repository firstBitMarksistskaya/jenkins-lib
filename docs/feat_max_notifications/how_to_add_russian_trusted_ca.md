# Настройка сертификатов для MAX API

## Зачем это нужно

`platform-api2.max.ru` использует TLS-сертификат российского корневого удостоверяющего центра (Минцифры), которого нет в стандартном trust store JVM. Без этого сертификата отправка уведомлений будет завершаться ошибкой:

```
unable to find valid certification path to requested target
```

Проблема решается добавлением сертификата в образ агента `base-jenkins-agent` (если для сборки используются скрипты из [onec-docker](https://github.com/firstBitMarksistskaya/onec-docker)).

## Что нам потребуется

- доступ к docker-registry, где лежит `base-jenkins-agent`.
- локально установленный `docker` с сетевым доступом к `gu-st.ru` (оттуда скачиваются сертификаты) и к вашему docker-registry.

## Как добавить сертификат в образ агента

### 1. Определить целевой образ

Один раз задаем адрес образа, в который будут добавлены сертификаты. Эта переменная будет использоваться в командах сборки и push.

```sh
BASE=<адрес-registry>/base-jenkins-agent:<версия-платформы>
```

### 2. Авторизоваться в registry

```sh
docker login <адрес-registry>
```

### 3. Собрать и запушить образ с сертификатами

Команда собирает поверх текущего `base-jenkins-agent` слой, в котором скачиваются сертификаты Минцифры и импортируются в системный `cacerts` JVM:

```sh
docker build --build-arg BASE=$BASE -t $BASE --pull - <<'EOF'
ARG BASE
FROM ${BASE}
USER root
RUN set -eux && \
    CACERTS="$(dirname $(dirname $(readlink -f $(which keytool))))/lib/security/cacerts" && \
    curl -sSL -o /tmp/root.crt https://gu-st.ru/content/lending/russian_trusted_root_ca_pem.crt && \
    curl -sSL -o /tmp/sub.crt  https://gu-st.ru/content/lending/russian_trusted_sub_ca_pem.crt && \
    (keytool -delete -alias mincifra-root -keystore "$CACERTS" -storepass changeit || true) && \
    (keytool -delete -alias mincifra-sub  -keystore "$CACERTS" -storepass changeit || true) && \
    keytool -import -trustcacerts -noprompt -alias mincifra-root -file /tmp/root.crt -keystore "$CACERTS" -storepass changeit && \
    keytool -import -trustcacerts -noprompt -alias mincifra-sub  -file /tmp/sub.crt  -keystore "$CACERTS" -storepass changeit && \
    rm /tmp/root.crt /tmp/sub.crt
EOF

docker push $BASE
```

Тег образа не меняется - агенты будут получать обновленный образ автоматически при следующем pull.

## Как обновить образ на агентах

- Kubernetes - на нодах может остаться кэшированная предыдущая версия. В настройках пода агента установите `imagePullPolicy: Always` (в UI Jenkins Kubernetes-плагина - галочка Always pull image).
- Docker Swarm - дополнительно ничего делать не нужно, при следующем запуске сервиса будет использован свежий образ.

Готово. Агент готов рассылать уведомления в MAX.