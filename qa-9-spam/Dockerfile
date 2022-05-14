FROM python:3.9-bullseye

WORKDIR /app
RUN python -m pip install pika --upgrade

COPY . .

CMD [ "python3", "script.py"]