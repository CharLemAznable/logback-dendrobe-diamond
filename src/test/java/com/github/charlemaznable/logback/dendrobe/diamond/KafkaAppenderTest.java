package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.logback.dendrobe.kafka.KafkaClientManager;
import com.github.charlemaznable.logback.dendrobe.kafka.KafkaClientManagerListener;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.n3r.diamond.client.impl.DiamondSubscriber;
import org.n3r.diamond.client.impl.MockDiamondServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static com.github.charlemaznable.core.codec.Json.unJson;
import static com.github.charlemaznable.core.kafka.KafkaClientElf.buildConsumer;
import static com.github.charlemaznable.core.kafka.KafkaClientElf.buildProducer;
import static com.github.charlemaznable.core.kafka.KafkaConfigElf.KAFKA_CONFIG_DIAMOND_GROUP_NAME;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.joor.Reflect.on;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class KafkaAppenderTest implements DiamondUpdaterListener, KafkaClientManagerListener {

    private static final String CLASS_NAME = KafkaAppenderTest.class.getName();

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

    static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    private static KafkaProducer<String, String> kafkaProducer;
    private static KafkaConsumer<String, String> kafkaConsumer;

    private static Logger root;
    private static Logger self;

    private boolean updated;
    private boolean configured;

    @BeforeAll
    public static void beforeAll() {
        kafka.start();
        val bootstrapServers = kafka.getBootstrapServers();

        val producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = buildProducer(producerConfig);

        val consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumer = buildConsumer(consumerConfig);
        kafkaConsumer.assign(Collections.singleton(new TopicPartition("logback.diamond", 0)));

        await().forever().until(() -> nonNull(
                DiamondSubscriber.getInstance().getDiamondRemoteChecker()));
        Object diamondRemoteChecker = DiamondSubscriber.getInstance().getDiamondRemoteChecker();
        await().forever().until(() -> 1 <= on(diamondRemoteChecker)
                .field("diamondAllListener").field("allListeners").call("size").<Integer>get());

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(KafkaAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        kafkaProducer.close();
        kafkaConsumer.close();
        kafka.stop();
    }

    @Test
    public void testKafkaAppender() {
        MockDiamondServer.setUpMockServer();
        DiamondUpdater.addListener(this);
        KafkaClientManager.addListener(this);

        updated = false;
        configured = false;
        MockDiamondServer.setConfigInfo(KAFKA_CONFIG_DIAMOND_GROUP_NAME, "DEFAULT",
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG + "=" + kafka.getBootstrapServers() + "\n");
        MockDiamondServer.updateDiamond("Logback", "test", "" +
                "root[console.level]=info\n" +
                CLASS_NAME + "[appenders]=[kafka]\n" +
                CLASS_NAME + "[kafka.level]=info\n" +
                CLASS_NAME + "[kafka.name]=DEFAULT\n" +
                CLASS_NAME + "[kafka.topic]=logback.diamond\n");
        await().forever().until(() -> updated);
        await().forever().until(() -> configured);

        root.info("root kafka log {}", "1");
        self.info("self kafka log {}", "1");
        await().timeout(Duration.ofSeconds(20)).untilAsserted(() ->
                assertConsumedContent("self kafka log 1"));

        KafkaClientManager.removeListener(this);
        DiamondUpdater.removeListener(this);
        MockDiamondServer.tearDownMockServer();
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private void assertConsumedContent(String content) {
        while (true) {
            val records = kafkaConsumer.poll(Duration.ofSeconds(1));
            for (val record : records) {
                val valueMap = unJson(record.value());
                assertEquals(content, ((Map<String, String>) valueMap.get("event")).get("message"));
                return;
            }
        }
    }

    @Override
    public void acceptDiamondStoneProperties(Properties properties) {
        updated = true;
    }

    @Override
    public void configuredKafkaClient(String kafkaName) {
        configured = true;
    }
}
