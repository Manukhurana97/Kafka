package com.example.kafka_basic.Consumer;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;


public class ConsumerDemo {
    public static final Logger log = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());

    public static void main(String[] args) {

        log.info("I'm a kafka consumer");

        //create new property
        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "Java_Consumer_group";
        String offsetReset = "earliest";
        String topic = "demo_java";

        // create a consumer config
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);


        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // consumer message
//        consumer.subscribe(Collections.singleton(topic)); // for single topic
        consumer.subscribe(Arrays.asList(topic));// for multiple topic

        // poll for new data

        while (true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));

            // iterating in records
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                log.info("key: " + consumerRecord.key() + ", value: " + consumerRecord.value());
                log.info("Partition: " + consumerRecord.partition() + ", Offset: " + consumerRecord.offset());
            }
        }

    }
}
