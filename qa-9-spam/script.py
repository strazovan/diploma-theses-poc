#!/usr/bin/env python
import pika
import json
import os

username=os.environ["RABBITMQ_USERNAME"]
password=os.environ["RABBITMQ_PASSWORD"]
host=os.environ["RABBITMQ_HOST"]
port=int(os.environ["RABBITMQ_PORT"])

credentials = pika.PlainCredentials(username, password)
connection = pika.BlockingConnection(pika.ConnectionParameters(host, port, '/', credentials))
channel = connection.channel()

for _ in range(1, 1000):
    channel.basic_publish(exchange='jobs-exchange',
                          routing_key='jobs.ping',
                          body='{ \"jobID\": \"dfa879sad7f\", \"jobType\": \"ping\", \"parameters\": { \"address\": \"fel.cvut.cz\" }}')

connection.close()