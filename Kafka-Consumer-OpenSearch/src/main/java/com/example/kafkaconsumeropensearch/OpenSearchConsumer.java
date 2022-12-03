package com.example.kafkaconsumeropensearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumer {

    //static String commString = "http://localhost:9200"; // locally
    static String commString = "https://x0zfv0wqpo:lb1rdsohpo@kafka-8670405712.eu-central-1.bonsaisearch.net:443"; // bonsai server
    static final String topic = "wikimedia.recentchange";

    public static RestHighLevelClient createOpenSearchClient() {
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(commString);
//         extract login  if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // Rest client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));
        } else {
            // Rest client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                                    new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                            )
            );

        }
        return restHighLevelClient;
    }

    public static KafkaConsumer<String, String> createKafkaConsumer() {
        String boostrapServers = "127.0.0.1:9092";
        String groupId = "consumer-opensearch-demo";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // checkpoints


        return new KafkaConsumer<>(properties);
    }

    public static String extractId(String json) {
        return JsonParser.
                parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }


    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());

        // first create an Open Search Client
        RestHighLevelClient openSearchClient = createOpenSearchClient();


        // create out kafka client
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // need to create the index on OpenSearch   if it doesn't exist already

        try (openSearchClient; consumer) {

            boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);

            if (!indexExists) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("The Wikimedia Index has been created!");
            } else {
                log.info("The Wikimedia Index already exits");
            }

            // we subscribe the consumer
            consumer.subscribe(Arrays.asList(topic));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                int recordsCount = records.count();
                log.info("Received {} record(s)", recordsCount);


                BulkRequest bulkRequest = new BulkRequest();

                for (ConsumerRecord<String, String> record : records) {
                    // sending records to open-search

                    // strategy 1
                    // define an ID using Kafka Record coordinates
                    // String id = record.topic() + "_" + record.partition() + "_" + record.offset();

                    try {

                        String id = extractId(record.value());

                        IndexRequest indexRequest = new IndexRequest("wikimedia").source(record.value(), XContentType.JSON).id(id);

                        bulkRequest.add(indexRequest);

                    } catch (Exception ignored) {

                    }
                }

                // storing teh data in bulk
                if (bulkRequest.numberOfActions() > 0) {
                    BulkResponse bulkItemResponses = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    log.info("Inserted {} record(s).", bulkItemResponses.getItems().length);
                }

                consumer.commitSync();
                log.info("Offsets have been committed! (manually as AutoOffsetResetIs::OFF)");

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // main code logic


        // close things
    }
}
