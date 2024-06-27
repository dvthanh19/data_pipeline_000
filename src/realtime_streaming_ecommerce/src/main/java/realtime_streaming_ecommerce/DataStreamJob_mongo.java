package realtime_streaming_ecommerce;

import Deserializer.JSONValueDeserializationSchema;
// import Dto.SalesPerCategory;
// import Dto.SalesPerDay;
// import Dto.SalesPerMonth;
import Dto.Transaction;

import static utils.JsonUtil.convertTransactionToJson;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.mongodb.sink.MongoSink;
import org.apache.flink.elasticsearch7.shaded.org.apache.http.HttpHost;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.action.index.IndexRequest;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.client.Requests;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.common.xcontent.XContentType;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch7SinkBuilder;

import org.apache.flink.connector.base.DeliveryGuarantee;
import com.mongodb.client.model.InsertOneModel;
import org.bson.BsonDocument;



public class DataStreamJob_mongo
{
    private static final String dbAdr = "localhost:27017";
    private static final String username = "mongo";
    private static final String password = "1234";
    private static final String mongoDbUri = "mongodb://" + username + ":" + password + "@" + dbAdr;
    
    private static final String dbName = "mydb";
    private static final String collectionName = "transactions";

    private static final String topic = "financial_transactions";

    public static void main(String[] args) throws Exception
    {
        System.out.println("[MongoDB] Starting...");

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        

        // =====================================================================================================================================
        // KAFKA SOURCE ========================================================================================================================
        KafkaSource<Transaction> source = KafkaSource.<Transaction>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics(topic)
                .setGroupId("flink-mysql-consumer")
                .setStartingOffsets(OffsetsInitializer.latest())    // ...modify: latest() or earliest() 
                .setValueOnlyDeserializer(new JSONValueDeserializationSchema())
                .build();

        
        // =====================================================================================================================================
        // DATASTREAM ==========================================================================================================================
        DataStream<Transaction> transactionStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka source");
        transactionStream.print();



        // =====================================================================================================================================
        // MONGOSINK SOURCE ====================================================================================================================
        MongoSink<Transaction> sink = MongoSink.<Transaction>builder()
                .setUri(mongoDbUri)
                .setDatabase(dbName)
                .setCollection(collectionName)
                .setBatchSize(1000)
                .setBatchIntervalMs(1000)
                .setMaxRetries(3)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setSerializationSchema(
                        (transaction, context) -> new InsertOneModel<>(BsonDocument.parse(convertTransactionToJson(transaction))))
                .build();

        transactionStream.sinkTo(sink).name("MongoDB Sink");


        // =====================================================================================================================================
        // ELASTICSEARCH SINK ==================================================================================================================
        transactionStream.sinkTo(
                new Elasticsearch7SinkBuilder<Transaction>()
                    .setHosts(new HttpHost("localhost", 9200, "http"))
                    .setEmitter((transaction, runtimeContext, requestIndexer) -> {
                        String transaction_json = convertTransactionToJson(transaction);
                        IndexRequest indexRequest = Requests
                            .indexRequest()
                            .index("transactions")
                            .id(transaction.getTransactionId())
                            .source(transaction_json, XContentType.JSON);

                        requestIndexer.add(indexRequest);
                    })
                    .build()
        )
        .name("Elasticsearch Sink");


        env.execute("[MongoDB] Flink Ecommerce Realtime Streaming");
    }
}