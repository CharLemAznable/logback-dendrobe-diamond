package com.github.charlemaznable.logback.dendrobe.diamond;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.OpenRequest;
import com.github.charlemaznable.core.es.EsConfig;
import com.github.charlemaznable.logback.dendrobe.es.EsClientManager;
import com.github.charlemaznable.logback.dendrobe.es.EsClientManagerListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.n3r.diamond.client.impl.DiamondSubscriber;
import org.n3r.diamond.client.impl.MockDiamondServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static com.github.charlemaznable.core.es.EsClientElf.buildElasticsearchClient;
import static com.github.charlemaznable.core.es.EsClientElf.closeElasticsearchApiClient;
import static com.github.charlemaznable.core.es.EsConfigElf.ES_CONFIG_DIAMOND_GROUP_NAME;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.joor.Reflect.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class EsAppenderTest implements DiamondUpdaterListener, EsClientManagerListener {

    private static final String CLASS_NAME = EsAppenderTest.class.getName();

    private static final String ELASTICSEARCH_VERSION = "7.17.6";
    private static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag(ELASTICSEARCH_VERSION);

    private static final String ELASTICSEARCH_USERNAME = "elastic";
    private static final String ELASTICSEARCH_PASSWORD = "changeme";

    private static final ElasticsearchContainer elasticsearch
            = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withPassword(ELASTICSEARCH_PASSWORD);

    private static ElasticsearchClient esClient;

    private static Logger root;
    private static Logger self;

    private boolean updated;
    private boolean configured;

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        elasticsearch.start();

        val esConfig = new EsConfig();
        esConfig.setUris(newArrayList(elasticsearch.getHttpHostAddress()));
        esConfig.setUsername(ELASTICSEARCH_USERNAME);
        esConfig.setPassword(ELASTICSEARCH_PASSWORD);
        esClient = buildElasticsearchClient(esConfig);

        esClient.indices().create(CreateIndexRequest.of(builder -> builder.index("logback.diamond")));
        esClient.indices().open(OpenRequest.of(builder -> builder.index("logback.diamond")));

        await().forever().until(() -> nonNull(
                DiamondSubscriber.getInstance().getDiamondRemoteChecker()));
        Object diamondRemoteChecker = DiamondSubscriber.getInstance().getDiamondRemoteChecker();
        await().forever().until(() -> 1 <= on(diamondRemoteChecker)
                .field("diamondAllListener").field("allListeners").call("size").<Integer>get());

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(EsAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        closeElasticsearchApiClient(esClient);
        elasticsearch.stop();
    }

    @Test
    public void testEsAppender() {
        MockDiamondServer.setUpMockServer();
        DiamondUpdater.addListener(this);
        EsClientManager.addListener(this);

        updated = false;
        configured = false;
        MockDiamondServer.setConfigInfo(ES_CONFIG_DIAMOND_GROUP_NAME, "DEFAULT", "" +
                "uris=" + elasticsearch.getHttpHostAddress() + "\n" +
                "username=" + ELASTICSEARCH_USERNAME + "\n" +
                "password=" + ELASTICSEARCH_PASSWORD + "\n");
        MockDiamondServer.updateDiamond("Logback", "test", "" +
                "root[console.level]=info\n" +
                CLASS_NAME + "[appenders]=[es]\n" +
                CLASS_NAME + "[es.level]=info\n" +
                CLASS_NAME + "[es.name]=DEFAULT\n" +
                CLASS_NAME + "[es.index]=logback.diamond\n");
        await().forever().until(() -> updated);
        await().forever().until(() -> configured);

        root.info("root es log {}", "1");
        self.info("self es log {}", "1");
        await().timeout(Duration.ofSeconds(20)).untilAsserted(() ->
                assertSearchContent("self es log 1"));

        EsClientManager.removeListener(this);
        DiamondUpdater.removeListener(this);
        MockDiamondServer.tearDownMockServer();
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    @SneakyThrows
    private void assertSearchContent(String content) {
        val searchRequest = SearchRequest.of(builder ->
                builder.index("logback.diamond").query(queryBuilder ->
                        queryBuilder.matchPhrase(phrase ->
                                phrase.field("event.message").query(content)
                        )
                )
        );
        val searchResponse = esClient.search(searchRequest, Map.class);
        val searchResponseHits = searchResponse.hits().hits();
        assertTrue(searchResponseHits.size() > 0);
        val responseMap = searchResponseHits.get(0).source();
        assertNotNull(responseMap);
        assertEquals(content, ((Map<String, String>) responseMap.get("event")).get("message"));
    }

    @Override
    public void acceptDiamondStoneProperties(Properties properties) {
        updated = true;
    }

    @Override
    public void configuredEsClient(String esName) {
        configured = true;
    }
}
