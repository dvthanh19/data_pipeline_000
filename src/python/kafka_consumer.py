from confluent_kafka import Consumer, KafkaException
import json

def consume_messages():
    # ...modify the bootstrap.servers
    # IP cty 172.16.8.19
    conf = {
        # 'bootstrap.servers': '172.16.8.19:9092',
        'bootstrap.servers': 'localhost:9092',
        'group.id': 'kafka_consumer',
        'auto.offset.reset': 'latest'
    }

    consumer = Consumer(conf)
    consumer.subscribe(['financial_transactions'])

    try:
        while True:
            msg = consumer.poll(0)

            if msg is None: continue
            if msg.error():
                print(msg.error())
                break
                # if msg.error().code() == KafkaException._PARTITION_EOF:
                #     continue
                # else:
                #     print(msg.error())
                #     break

            try:
                transaction = json.loads(msg.value().decode('utf-8'))
                print(f'Received message: {transaction}')

            except Exception as e:
                print(f'Error decoding message: {e}')

    except KeyboardInterrupt:
        pass

    finally:
        consumer.close()


if __name__ == '__main__':
    consume_messages()