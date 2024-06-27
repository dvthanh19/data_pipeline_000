docker compose -f docker-compose.yml -p demo up -d
docker-compose -p demo down

python3 kafka_prodcuer.py
python3 kafka_consumer.py

./bin/start-cluster.sh
./bin/stop-cluster.sh

./bin/flink list -r
./bin/flink cancel \<job_id\>



~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar


---------------------------------------------------------------------------------------------------------------------------------------
Postgres:
~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob_postgres target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar

psql -U postgres

---------------------------------------------------------------------------------------------------------------------------------------
MySQL:
~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob_mysql target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar

mysql -h localhost -P 3306 -u mysql -p \<optional database_name\>

---------------------------------------------------------------------------------------------------------------------------------------
Mongo DB:
~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob_mongo target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar

mongosh --username mongo --password 1234 --authenticationDatabase admin  
show dbs  
use /<db_name/>  
db.\<collection_name\>.find(\<(optional) field: value)\>  
db.\<collection_name\>.drop()