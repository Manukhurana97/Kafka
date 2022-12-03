package com.example.kafkaproducerwikimedia;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;


import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class WikimediaChangesProducer {
    public static void main(String[] args) throws InterruptedException {
        final String bootstrapServer = "127.0.0.1:9092";
        final String topic = "wikimedia.recentchange";
        final String wikimediaStreamURL = "https://stream.wikimedia.org/v2/stream/recentchange";

        // creating Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // set safe kafka config
//        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
//        properties.setProperty(ProducerConfig.ACKS_CONFIG, "-1");
//        properties.setProperty(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, Integer.toString(Integer.MAX_VALUE));
//        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        // set high throughput Producer config
//        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
//        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
//        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024));

        // create the producer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(properties);


        EventHandler eventHandler = new WikimediaChangeHandler(kafkaProducer, topic);
        EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(wikimediaStreamURL));
        EventSource eventSource = builder.build();


        // start the producer in the another thread
        eventSource.start();

        // produce for 10 min and block the program until then
        TimeUnit.MINUTES.sleep(10);
    }
}
