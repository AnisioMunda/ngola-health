FROM postgres:18-alpine

RUN apk add --no-cache age bash coreutils

COPY backup.sh /usr/local/bin/backup.sh
COPY restore.sh /usr/local/bin/restore.sh
COPY scheduler.sh /usr/local/bin/scheduler.sh

RUN chmod 0755 \
    /usr/local/bin/backup.sh \
    /usr/local/bin/restore.sh \
    /usr/local/bin/scheduler.sh

CMD ["/usr/local/bin/scheduler.sh"]
