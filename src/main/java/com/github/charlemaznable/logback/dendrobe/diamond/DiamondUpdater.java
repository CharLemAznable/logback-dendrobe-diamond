package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.logback.dendrobe.HotUpdater;
import com.github.charlemaznable.logback.dendrobe.LogbackDendrobeListener;
import com.google.auto.service.AutoService;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import lombok.val;
import org.n3r.diamond.client.DiamondAxis;
import org.n3r.diamond.client.DiamondListener;
import org.n3r.diamond.client.DiamondStone;
import org.n3r.diamond.client.Miner;
import org.n3r.diamond.client.impl.DiamondSubscriber;
import org.slf4j.helpers.Reporter;

import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.charlemaznable.core.lang.Propertiess.parseStringToProperties;
import static java.util.concurrent.Executors.newFixedThreadPool;

@AutoService(HotUpdater.class)
public final class DiamondUpdater implements HotUpdater, DiamondListener {

    private static final CopyOnWriteArrayList<DiamondUpdaterListener> listeners = new CopyOnWriteArrayList<>();
    private static final AsyncEventBus notifyBus;

    private static final String DIAMOND_GROUP_KEY = "logback.diamond.group";
    private static final String DIAMOND_DATA_ID_KEY = "logback.diamond.dataId";

    private static final String DEFAULT_GROUP = "Logback";
    private static final String DEFAULT_DATA_ID = "default";

    private LogbackDendrobeListener dendrobeListener;

    static {
        notifyBus = new AsyncEventBus(DiamondUpdaterListener.class.getName(), newFixedThreadPool(1));
        notifyBus.register(new Object() {
            @Subscribe
            public void notifyListeners(Properties properties) {
                for (val listener : listeners) {
                    try {
                        listener.acceptDiamondStoneProperties(properties);
                    } catch (Exception t) {
                        Reporter.error("listener error:", t);
                    }
                }
            }
        });
    }

    public static void addListener(DiamondUpdaterListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(DiamondUpdaterListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void initialize(LogbackDendrobeListener listener, Properties config) {
        this.dendrobeListener = listener;

        // 本地配置diamond配置坐标
        val group = config.getProperty(DIAMOND_GROUP_KEY, DEFAULT_GROUP);
        val dataId = config.getProperty(DIAMOND_DATA_ID_KEY, DEFAULT_DATA_ID);

        new Thread(() -> {
            // diamond配置覆盖默认配置
            val diamondAxis = DiamondAxis.makeAxis(group, dataId);
            val diamondStone = new DiamondStone();
            diamondStone.setContent(new Miner().getStone(group, dataId));
            diamondStone.setDiamondAxis(diamondAxis);
            accept(diamondStone);

            DiamondSubscriber.getInstance().
                    addDiamondListener(diamondAxis, DiamondUpdater.this);
        }).start();
    }

    @Override
    public void accept(DiamondStone diamondStone) {
        val properties = parseStringToProperties(diamondStone.getContent());
        this.dendrobeListener.reset(properties);
        notifyBus.post(properties);
    }
}
