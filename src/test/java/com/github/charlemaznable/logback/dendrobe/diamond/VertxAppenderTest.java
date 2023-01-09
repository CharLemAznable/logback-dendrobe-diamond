package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.core.vertx.VertxElf;
import com.github.charlemaznable.logback.dendrobe.vertx.VertxManager;
import com.github.charlemaznable.logback.dendrobe.vertx.VertxManagerListener;
import com.hazelcast.config.Config;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.n3r.diamond.client.impl.DiamondSubscriber;
import org.n3r.diamond.client.impl.MockDiamondServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

import static com.github.charlemaznable.core.vertx.VertxClusterConfigElf.VERTX_CLUSTER_CONFIG_DIAMOND_GROUP_NAME;
import static com.github.charlemaznable.core.vertx.VertxOptionsConfigElf.VERTX_OPTIONS_DIAMOND_GROUP_NAME;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.joor.Reflect.on;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class VertxAppenderTest implements DiamondUpdaterListener, VertxManagerListener {

    private static final String CLASS_NAME = VertxAppenderTest.class.getName();

    private static Vertx vertx;
    private static String lastEventMessage;
    private static Logger root;
    private static Logger self;

    private boolean updated;
    private boolean configured;

    @BeforeAll
    public static void beforeAll() {
        await().forever().until(() -> nonNull(
                DiamondSubscriber.getInstance().getDiamondRemoteChecker()));
        Object diamondRemoteChecker = DiamondSubscriber.getInstance().getDiamondRemoteChecker();
        await().forever().until(() -> 1 <= on(diamondRemoteChecker)
                .field("diamondAllListener").field("allListeners").call("size").<Integer>get());
        val vertxOptions = new VertxOptions();
        vertxOptions.setWorkerPoolSize(10);
        val hazelcastConfig = new Config();
        hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        vertxOptions.setClusterManager(new HazelcastClusterManager(hazelcastConfig));
        vertx = VertxElf.buildVertx(vertxOptions);
        vertx.eventBus().consumer("logback.diamond",
                (Handler<Message<JsonObject>>) event -> {
                    try {
                        lastEventMessage = event.body().getJsonObject("event").getString("message");
                    } catch (Exception e) {
                        lastEventMessage = null;
                    }
                });

        root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        self = LoggerFactory.getLogger(VertxAppenderTest.class);
    }

    @AfterAll
    public static void afterAll() {
        VertxElf.closeVertx(vertx);
    }

    @Test
    public void testVertxAppender() {
        MockDiamondServer.setUpMockServer();
        DiamondUpdater.addListener(this);
        VertxManager.addListener(this);

        updated = false;
        configured = false;
        MockDiamondServer.setConfigInfo(VERTX_CLUSTER_CONFIG_DIAMOND_GROUP_NAME, "DEFAULT", "" +
                "hazelcast:\n" +
                "  network:\n" +
                "    join:\n" +
                "      multicast:\n" +
                "        enabled: true\n");
        MockDiamondServer.setConfigInfo(VERTX_OPTIONS_DIAMOND_GROUP_NAME, "DEFAULT", "" +
                "workerPoolSize=42\n" +
                "clusterManager=@com.github.charlemaznable.vertx.config.DiamondHazelcastClusterManager(DEFAULT)\n");
        MockDiamondServer.updateDiamond("Logback", "test", "" +
                "root[console.level]=info\n" +
                CLASS_NAME + "[appenders]=[vertx]\n" +
                CLASS_NAME + "[vertx.level]=info\n" +
                CLASS_NAME + "[vertx.name]=DEFAULT\n" +
                CLASS_NAME + "[vertx.address]=logback.diamond\n" +
                CLASS_NAME + "[console.level]=off\n" +
                CLASS_NAME + "[eql.level]=off\n");
        await().forever().until(() -> updated);
        await().forever().until(() -> configured);

        root.info("root vertx log {}", "old");
        self.info("self vertx log {}", "old");
        await().timeout(Duration.ofSeconds(20)).untilAsserted(() ->
            assertEquals("self vertx log old", lastEventMessage));

        VertxManager.removeListener(this);
        DiamondUpdater.removeListener(this);
        MockDiamondServer.tearDownMockServer();
    }

    @Override
    public void acceptDiamondStoneProperties(Properties properties) {
        updated = true;
    }

    @Override
    public void configuredVertx(String vertxName) {
        configured = true;
    }
}
