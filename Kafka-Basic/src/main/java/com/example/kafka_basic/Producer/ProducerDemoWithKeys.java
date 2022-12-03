package com.example.kafka_basic.Producer;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithKeys {

    public static final Logger log = LoggerFactory.getLogger(ProducerDemoWithKeys.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I'm a kafka producer with Keys");

        // create producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // crete producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // create a producer records
        for (int i = 0; i < 100; i++) {

            String topic = "demo_java";
            String key = "id_" + i;
            String value = "hello world " + i;

            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);


            // send data = async operation
            producer.send(producerRecord, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    // execute when the mgs is successfully sent  or an exception is thrown
                    if (e == null) {
                        // record was successfully sent
                        log.info("Receive new Message/ \n" +
                                "Topic: " + recordMetadata.topic() + " \n" +
                                "Key: " + producerRecord.key() + " \n" +
                                "Partition: " + recordMetadata.partition() + " \n" +
                                "offset: " + recordMetadata.offset() + " \n" +
                                "TimeStamp: " + recordMetadata.timestamp());
                    } else {
                        log.error("error while producing: " + e);
                    }
                }
            });
        }

        // flush Synchronized
        producer.flush();

        // close kafka connection
        producer.close();
    }

}
