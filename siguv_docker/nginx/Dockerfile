FROM nginx:alpine

ENV http_proxy="http://10.33.176.21:3128"
ENV https_proxy="http://10.33.176.21:3128"

COPY siguv.de.* /etc/tls/
COPY keycloak.conf /etc/nginx/conf.d/default.conf

RUN apk update --no-cache && \
    apk add openssl --no-cache && \
    cat /etc/tls/siguv.de.crt /etc/tls/siguv.de.ca > /etc/tls/siguv.de.chain.crt
