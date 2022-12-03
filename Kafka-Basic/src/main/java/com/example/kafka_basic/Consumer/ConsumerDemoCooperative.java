package com.example.kafka_basic.Consumer;


import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;


public class ConsumerDemoCooperative {
    public static final Logger log = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getSimpleName());

    public static void main(String[] args) {

        log.info("I'm a kafka consumer with Cooperative");

        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "Java_Consumer_group";
        String offsetReset = "earliest";
        String topic = "demo_java";


        // create a consumer config Properties
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        properties.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
//        properties.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "");


        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);


        // got a reference on my current thread
        final Thread mainThread = Thread.currentThread();


        // adding shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info(" Detected Shutdown, let's exits by consumer.wakeup()...");
                consumer.wakeup();

                /*
                 * need to wait till the main thread complete in while statement
                 */

                // join the main thread to allow the execution of the code to the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        try {

            // consumer message
            // consumer.subscribe(Collections.singleton(topic)); // for single topic
            consumer.subscribe(Arrays.asList(topic));// for multiple topic

            // poll for new data
            while (true) {
                log.info("polling...");
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));

                // iterating in records
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    log.info("key: " + consumerRecord.key() + ", value: " + consumerRecord.value());
                    log.info("Partition: " + consumerRecord.partition() + ", Offset: " + consumerRecord.offset());
                }
            }
        } catch (WakeupException e) {
            log.info("Wake up exception");
        } catch (Exception e) {
            log.error("Error Expired {}", e.getMessage());
        } finally {
            consumer.close(); // this will also commit the offsets if need be
            log.info("Consumer is gratefully closed");
        }
    }
}
