
**DOCKER**
```shell
# Up docker containers
docker compose -f docker-compose.yml -p demo up -d

# Down docker containers
docker-compose -p demo down
```


**APACHE KAFKA**
```shell
# Start Producer
python3 kafka_producer.py

# Start Consumer
python3 kafka_consumer.py   
```


**APACHE FLINK**
```shell
# Start Flink
./bin/start-cluster.sh

# Stop Flink
./bin/stop-cluster.sh

# List all jobs
./bin/flink list -r
./bin/flink list -a

# Cancel job
./bin/flink cancel \<job_id\>
```


**MAVEN**
```shell
mvn clean
mvn compile && mvn package
```

**SUBMIT FLINK TASKS**
```shell
# Postgres Job
~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob_postgres target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar

# MySQL Job
~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob_mysql target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar

# MongoDB Job
~/flink-1.19.1/bin/flink run -c realtime_streaming_ecommerce.DataStreamJob_mongo target/realtime_streaming_ecommerce-1.0-SNAPSHOT.jar
```


**POSTGRESQL**
```shell
# Start Postgres
psql -U postgres
```

```sql
-- List all databases
\l

-- Use the db_name database:  \c <db_name>
\c mydb

-- List all tables in 1 database
\l

-- Show contents of table:
select * from transactions
```


**MYSQL**
```shell
# Start Postgres: mysql -h localhost -P 3306 -u mysql -p <optional database_name\
mysql -h localhost -P 3306 -u mysql -p mydb
```

```sql
-- List all databases
show databases;

-- Use the db_name database
use mydb;

-- List all tables in 1 database
show tables;

-- Show contents of table:
select * from transactions;
```


**MONGODB**
```shell
# Start MongoDB: mongosh --username <username> --password <password> --authenticationDatabase admin  
mongosh --username mongo --password 1234 --authenticationDatabase admin  
```
```sql
-- List all databases
show databases
show dbs

-- Use database: use <db_name>
use mydb

-- Show all collections
show collections


-- List all content of 
-- Show contents: db.\<collection_name\>.find(\<(optional) field: value)\> 
db.mydb.find()
db.mydb.find({...: ...})

-- Drop collection: db.\<collection_name\>.drop()
db.transactions.drop()

-- Drop database: db.dropDatabase()
use mydb
db.dropDatabase()
```
