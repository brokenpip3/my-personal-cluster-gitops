FROM alpine:latest

RUN apk --no-cache add curl bash tar

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
USER 1001
ENTRYPOINT ["/entrypoint.sh"]
