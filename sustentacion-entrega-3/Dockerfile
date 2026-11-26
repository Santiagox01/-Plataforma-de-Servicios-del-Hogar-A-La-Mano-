FROM python:3.12-alpine

WORKDIR /app

RUN addgroup -S deck && adduser -S deck -G deck

COPY --chown=deck:deck index.html styles.css app.js voxel-fx.js serve.py ./
COPY --chown=deck:deck assets ./assets

USER deck

ENV HOST=0.0.0.0
ENV PORT=8080
EXPOSE 8080

CMD ["python", "serve.py"]
