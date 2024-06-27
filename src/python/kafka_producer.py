from faker import Faker
from confluent_kafka import SerializingProducer

from generate_data import generate_sales_transactions

import json
import random
import time
from datetime import datetime




def delivery_report(err, msg):
    if err is not None:
        print(f'Message delivery failed: {err}')
    else:
        print(f'Message delivered to {msg.topic} [{msg.partition}]')


def sendData():
    topic = 'financial_transactions'
    # ...modify the bootstrap.servers
    # IP cty 172.16.8.19
    # producer = SerializingProducer({'bootstrap.servers': '172.16.8.19:9092'})
    producer = SerializingProducer({'bootstrap.servers': 'localhost:9092'})

    curr_time = datetime.now()
    while (datetime.now() - curr_time).seconds < 120:
        try:
            transaction = generate_sales_transactions()
            transaction['totalCost'] = transaction['productPrice'] * transaction['productQuantity']
            
            print(transaction)
            producer.produce(
                topic,
                key=transaction['transactionId'],
                value=json.dumps(transaction),
                on_delivery=delivery_report
            )

            producer.poll(0)
            time.sleep(3)       # wait 5 seconds before sending the next message


        except BufferError:
            print("Buffer full! Waiting...")
            time.sleep(1)
        except Exception as e:
            print(e)



if __name__ == '__main__':
    sendData()