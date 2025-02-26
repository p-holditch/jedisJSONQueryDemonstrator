import com.google.gson.Gson;
import redis.clients.jedis.*;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.Schema.Field;
import redis.clients.jedis.search.Schema.FieldType;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;



import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;


public class RedisClient {

    private final JedisPooled jedisPooled;
    private final Gson gson = new Gson();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final String indexName = "booking_index";

    public RedisClient(String host, int port) {

        this.jedisPooled = new JedisPooled(host, port);

    }

    public void populateRedis() {

        Random rand = new Random();
        LocalDate today = LocalDate.now();


        for (int i = 1; i < 101; i++) {

            // Store in Redis
            String key = "booking:" + i;
            Booking booking = new Booking(i, "location_" + (i % 10),
                    today.withDayOfYear(i).toString(),
                    today.withDayOfYear(i + rand.nextInt(3)).toString(),
                    "Name_" + (i % 40),
                    "Customer" + (i%40) + "@email.com",
                    (i % 40));
            this.jedisPooled.jsonSet(key, gson.toJson(booking));

        }



        // Drop the index if it already exists
        try {
            this.jedisPooled.ftDropIndex(indexName);
            System.out.println("Index dropped.");
        } catch (Exception e) {
            System.out.println("Index does not exist. Proceeding to create.");
        }

        // Define the index schema
        Schema schema = new Schema()
                .addField(new Field("$.location", FieldType.TEXT)).as("location")
                .addField(new Field("$.customerName", FieldType.TEXT)).as("customerName")
                .addField(new Field("$.customerEmail", FieldType.TAG)).as("customerEmail");

        try {
            // Create the index
        this.jedisPooled.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes("booking:")), schema);
        System.out.println("Index created on fields: location, customerName");
        } catch(Exception e) {
            System.err.println("Error creating index: " + e.getMessage());
        }
    }


    public void executeRedisCommands() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(5)) {

            for (int i = 0; i < 5; i++) {
                final int threadId = i;

                executorService.submit(() -> {
                    try {
                        // Generate string dates
                        String startDate = LocalDate.now().format(dateFormatter);
                        String endDate = LocalDate.now().plusDays(7).format(dateFormatter);

                        // Create a Booking object
                        Booking booking = new Booking(
                                1000 + threadId,
                                "Location_" + threadId,
                                startDate,
                                endDate,
                                "Customer_" + (threadId + 1),
                                "Customer+" + (threadId + 1) + "@email.com" ,
                                threadId + 1
                        );


                        // Store it in Redis
                        String key = "booking:" + (1000 + threadId);
                        this.jedisPooled.jsonSet(key, gson.toJson(booking));

                        System.out.println("Thread " + threadId + " - Retrieved Booking: " + this.jedisPooled.jsonGet(key).toString());

                        // either count bookings per location, or bookings per customer

                        AggregationResult aggregationResponse;
                        String fieldName;
                        String countName;
                        if ( threadId%2 ==0 ) {
                            fieldName="customerName";
                            countName="bookings_per_customer";
                        } else {
                            fieldName="location";
                            countName="bookings_per_location";
                        }

                        aggregationResponse = this.jedisPooled.ftAggregate(indexName,
                                new AggregationBuilder("*").groupBy("@" + fieldName,
                                        Reducers.count().as(countName)));

                        for (Row resCountRow: aggregationResponse.getRows()) {
                            System.out.println(String.format(
                                    "Thread-%d: %s: %s - %d bookings",
                                    threadId, fieldName, resCountRow.getString(fieldName), resCountRow.getLong(countName))
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("Error in Thread " + threadId + ": " + e.getMessage());
                    }
                });
            }

            executorService.shutdown();
            while (!executorService.isTerminated()) {
                // Wait for all threads to finish
            }

            SearchResult sr = this.jedisPooled.ftSearch(indexName, new Query("@customerEmail:{\"Customer10@email.com\"}").dialect(2));
            System.out.println("There are " + sr.getTotalResults() + " documents with email Customer10@email.com:");
            List<Document> bookingList = sr.getDocuments();
            for (Document doc: bookingList) {
                System.out.println(doc.getId());
            }

            sr = this.jedisPooled.ftSearch(indexName, new Query("@customerEmail:{\"jane.aberg@gmail.com\"}").dialect(2));
            System.out.println("There are " + sr.getTotalResults() + " documents with email jane.aberg@gmail.com:");
            bookingList = sr.getDocuments();
            for (Document doc: bookingList) {
                System.out.println(doc.getId());
            }

        } catch( Exception e) {
            System.err.println("Problem creating threads: " + e.getMessage());
        }

    }
    public static void main(String[] args) {
        RedisClient client = new RedisClient("localhost", 6379);
        client.populateRedis();
        client.executeRedisCommands();


    }
}