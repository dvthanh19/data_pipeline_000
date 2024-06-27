package realtime_streaming_ecommerce;

import Deserializer.JSONValueDeserializationSchema;
import Dto.SalesPerCategory;
import Dto.SalesPerDay;
import Dto.SalesPerMonth;
import Dto.Transaction;

import static utils.JsonUtil.convertTransactionToJson;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.elasticsearch7.shaded.org.apache.http.HttpHost;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.action.index.IndexRequest;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.client.Requests;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.common.xcontent.XContentType;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch7SinkBuilder;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.jdbc.JdbcSink;
// import org.apache.flink.api.connector.sink.Sink;

import java.sql.Date;



public class DataStreamJob_mysql
{
    private static final String db_addr = "localhost:3306";
    private static final String db_name = "mydb";
    private static final String jdbcUrl = "jdbc:mysql://" + db_addr + "/" + db_name;
    
    private static final String username = "mysql";
    private static final String password = "1234";
    private static final String driverName = "com.mysql.cj.jdbc.Driver";

    private static final String topic = "financial_transactions";

    public static void main(String[] args) throws Exception
    {
        System.out.println("[MySQL] Starting...");

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


        // JDBC POSTGRES SINK ==================================================================================================================
        // Execute options ----------------------------------------------------------------------------
        JdbcExecutionOptions execOpts = new JdbcExecutionOptions.Builder()
                .withBatchSize(1000)
                .withBatchIntervalMs(200)
                .withMaxRetries(3)
                .build();

        // Connection options -------------------------------------------------------------------------
        JdbcConnectionOptions connOpts = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(jdbcUrl)
                .withDriverName(driverName)
                .withUsername(username)
                .withPassword(password)
                .build();

        // CREATE TABLE -------------------------------------------------------------------------------
        // Create transaction table in Postgres -------------------------------------------------------
        String query = 
        "CREATE TABLE IF NOT EXISTS transactions (" +
            "transaction_id VARCHAR(255) PRIMARY KEY, " +
            "product_id VARCHAR(255), " +
            "product_name VARCHAR(255), " +
            "product_category VARCHAR(255), " +
            "product_price DOUBLE PRECISION, " +
            "product_quantity INTEGER, " +
            "product_brand VARCHAR(255), " +
            "total_amount DOUBLE PRECISION, " +
            "currency VARCHAR(255), " +
            "customer_id VARCHAR(255), " +
            "transaction_date TIMESTAMP, " +
            "payment_method VARCHAR(255) " +
        ");";

        transactionStream.addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {},
                execOpts,
                connOpts
            ))
            .name("Create sink table transactions");

        
        // Create sales_per_category table in Postgres ------------------------------------------------
        query = 
        "CREATE TABLE IF NOT EXISTS sales_per_category (" +
            "transaction_date DATE, " +
            "category VARCHAR(255), " +
            "total_sales DOUBLE PRECISION, " +
            "PRIMARY KEY (transaction_date, category)" +
        ")";

        transactionStream.addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {},
                execOpts,
                connOpts
            )).name("Create Sales Per Category Table");


        // Create sales_per_day table -----------------------------------------------------------------
        query = 
        "CREATE TABLE IF NOT EXISTS sales_per_day (" +
            "transaction_date DATE PRIMARY KEY, " +
            "total_sales DOUBLE PRECISION " +
        ")";

        transactionStream.addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {},
                execOpts,
                connOpts
            ))
            .name("Create Sales Per Day Table");

        
        // Create sales_per_month table in Postgres ---------------------------------------------------
        query = "CREATE TABLE IF NOT EXISTS sales_per_month (" +
            "year INTEGER, " +
            "month INTEGER, " +
            "total_sales DOUBLE PRECISION, " +
            "PRIMARY KEY (year, month)" +
        ")";

        transactionStream.addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {},
                execOpts,
                connOpts
            ))
            .name("Create Sales Per Month Table");




        // ADD DATA -----------------------------------------------------------------------------------
        // Add Data into transactions table -----------------------------------------------------------
        query = 
        "INSERT INTO transactions(transaction_id, product_id, product_name, product_category, product_price," + 
                        "product_quantity, product_brand, total_amount, currency, customer_id," + 
                        "transaction_date, payment_method) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " + 
        "ON DUPLICATE KEY UPDATE " + 
            "product_id = VALUES(product_id), " + 
            "product_name = VALUES(product_name), " + 
            "product_category = VALUES(product_category), " + 
            "product_price = VALUES(product_price), " + 
            "product_quantity = VALUES(product_quantity), " +
            "product_brand = VALUES(product_brand), " + 
            "total_amount = VALUES(total_amount), " +
            "currency = VALUES(currency), " + 
            "customer_id = VALUES(customer_id), " +
            "transaction_date = VALUES(transaction_date)," + 
            "payment_method = VALUES(payment_method);";
        
        transactionStream.addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {
                    preparedStatement.setString(1, transaction.getTransactionId());
                    preparedStatement.setString(2, transaction.getProductId());
                    preparedStatement.setString(3, transaction.getProductName());
                    preparedStatement.setString(4, transaction.getProductCategory());
                    preparedStatement.setDouble(5, transaction.getProductPrice());
                    preparedStatement.setInt(6, transaction.getProductQuantity());
                    preparedStatement.setString(7, transaction.getProductBrand());
                    preparedStatement.setDouble(8, transaction.getTotalCost());
                    preparedStatement.setString(9, transaction.getCurrency());
                    preparedStatement.setString(10, transaction.getCustomerId());
                    preparedStatement.setTimestamp(11, transaction.getTransactionDate());
                    preparedStatement.setString(12, transaction.getPaymentMethod());
                },
                execOpts,
                connOpts
            ))
            .name("Insert data into sink table transactions");
        

        // Add Data into sale_per_category table -----------------------------------------------------------
        query = 
        "INSERT INTO sales_per_category(transaction_date, category, total_sales) " + 
            "VALUES (?, ?, ?) " + 
            "ON DUPLICATE KEY UPDATE " +
            "total_sales = VALUES(total_sales)";

        transactionStream.map(transaction -> {
                Date transactionDate = new Date(System.currentTimeMillis());
                String category = transaction.getProductCategory();
                double totalSales = transaction.getTotalCost();

                return new SalesPerCategory(transactionDate, category, totalSales);
            })
            .keyBy(SalesPerCategory::getCategory)
            .reduce((salesPerCategory, t1) -> {
                salesPerCategory.setTotalSales(salesPerCategory.getTotalSales() + t1.getTotalSales());
                
                return salesPerCategory;
            })
            .addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<SalesPerCategory>) (preparedStatement, salesPerCategory) -> {
                    preparedStatement.setDate(1, new Date(System.currentTimeMillis()));
                    preparedStatement.setString(2, salesPerCategory.getCategory());
                    preparedStatement.setDouble(3, salesPerCategory.getTotalSales());
                },
                execOpts,
                connOpts
            ))
            .name("Insert into sales per category table");
            


        // Add data into sale_per_day table ------------------------------------------------------
        query = 
        "INSERT INTO sales_per_day(transaction_date, total_sales) " +
            "VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_sales = VALUES(total_sales) ";

        transactionStream.map(transaction -> {
                Date transactionDate = new Date(System.currentTimeMillis());
                double totalSales = transaction.getTotalCost();

                return new SalesPerDay(transactionDate, totalSales);
            })
            .keyBy(SalesPerDay::getTransactionDate)
            .reduce((salesPerDay, t1) -> {
                salesPerDay.setTotalSales(salesPerDay.getTotalSales() + t1.getTotalSales());
                
                return salesPerDay;
            })
            .addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<SalesPerDay>) (preparedStatement, salesPerDay) -> {
                    preparedStatement.setDate(1, new Date(System.currentTimeMillis()));
                    preparedStatement.setDouble(2, salesPerDay.getTotalSales());
                },
                execOpts,
                connOpts
            ))
            .name("Insert into sales per day table");
        

        // Add Data into sale_per_month table ---------------------------------------------------------
        query = 
        "INSERT INTO sales_per_month(year, month, total_sales) " +
            "VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_sales = VALUES(total_sales) ";

        transactionStream.map(transaction -> {
                Date transactionDate = new Date(System.currentTimeMillis());
                int year = transactionDate.toLocalDate().getYear();
                int month = transactionDate.toLocalDate().getMonth().getValue();
                double totalSales = transaction.getTotalCost();

                return new SalesPerMonth(year, month, totalSales);
            })
            .keyBy(SalesPerMonth::getMonth)
            .reduce((salesPerMonth, t1) -> {
                salesPerMonth.setTotalSales(salesPerMonth.getTotalSales() + t1.getTotalSales());
                
                return salesPerMonth;
            })
            .addSink(JdbcSink.sink(
                query,
                (JdbcStatementBuilder<SalesPerMonth>) (preparedStatement, salesPerMonth) -> {
                    preparedStatement.setInt(1, salesPerMonth.getYear());
                    preparedStatement.setInt(2, salesPerMonth.getMonth());
                    preparedStatement.setDouble(3, salesPerMonth.getTotalSales());
                },
                execOpts,
                connOpts
            ))
            .name("Insert into sales per month table");
        

        
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



        // =====================================================================================================================================
        // SUBMIT JOB ==========================================================================================================================
        env.execute("[MySQL] Flink Ecommerce Realtime Streaming");
    }
}