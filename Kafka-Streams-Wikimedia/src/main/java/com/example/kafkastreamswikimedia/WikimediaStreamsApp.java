package com.example.kafkastreamswikimedia;

import com.example.kafkastreamswikimedia.Process.BotCountStreamBuilder;
import com.example.kafkastreamswikimedia.Process.EventCountTimeseriesBuilder;
import com.example.kafkastreamswikimedia.Process.WebsiteCountStreamBuilder;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.stream.Stream;

public class WikimediaStreamsApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikimediaStreamsApp.class.getSimpleName());
    private static final Properties properties;
    private static final String INPUT_TOPIC = "wikimedia.recentchange";
    private static final String BOOTSTARP_SERVER = "127.0.0.1:9092";


    static {
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "wikimedia-stats-application");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTARP_SERVER);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    }


    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> changeJsonStream = builder.stream(INPUT_TOPIC);

        BotCountStreamBuilder countStreamBuilder = new BotCountStreamBuilder(changeJsonStream);
        countStreamBuilder.setup();

        WebsiteCountStreamBuilder websiteCountStreamBuilder = new WebsiteCountStreamBuilder(changeJsonStream);
        websiteCountStreamBuilder.setup();

        EventCountTimeseriesBuilder eventCountTimeseriesBuilder = new EventCountTimeseriesBuilder(changeJsonStream);
        eventCountTimeseriesBuilder.setup();

        final Topology topology = builder.build();
        LOGGER.info("Topology: {}", topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, properties);
        streams.start();
    }
}
