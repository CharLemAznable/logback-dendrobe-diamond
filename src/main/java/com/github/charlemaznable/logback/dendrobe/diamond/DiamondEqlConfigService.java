package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.logback.dendrobe.eql.EqlConfigService;
import com.google.auto.service.AutoService;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.n3r.diamond.client.DiamondListener;
import org.n3r.diamond.client.DiamondManager;
import org.n3r.diamond.client.Miner;
import org.n3r.eql.config.EqlConfig;
import org.n3r.eql.config.EqlConfigManager;
import org.n3r.eql.config.EqlPropertiesConfig;
import org.n3r.eql.config.EqlTranFactoryCacheLifeCycle;
import org.n3r.eql.impl.DefaultEqlConfigDecorator;

import java.util.Properties;

import static com.github.charlemaznable.core.lang.Propertiess.parseStringToProperties;
import static com.github.charlemaznable.core.lang.Propertiess.tryDecrypt;
import static org.n3r.eql.config.EqlDiamondConfig.EQL_CONFIG_GROUP_NAME;

@AutoService(EqlConfigService.class)
public final class DiamondEqlConfigService implements EqlConfigService {

    @Override
    public String getEqlConfigValue(String configKey) {
        return new Miner().getStone(EQL_CONFIG_GROUP_NAME, configKey);
    }

    @Override
    public EqlConfig parseEqlConfig(String configKey, String configValue) {
        val properties = tryDecrypt(parseStringToProperties(configValue), configKey);
        if (properties.isEmpty()) return null;
        return new DiamondEqlConfig(properties, configKey);
    }

    @EqualsAndHashCode(of = "connectionName", callSuper = false)
    public static class DiamondEqlConfig extends EqlPropertiesConfig
            implements EqlTranFactoryCacheLifeCycle {

        private final String connectionName;
        private DiamondListener diamondListener;
        private DiamondManager diamondManager;

        public DiamondEqlConfig(Properties properties, String connectionName) {
            super(properties);
            this.connectionName = connectionName;
        }

        @Override
        public void onLoad() {
            diamondManager = new DiamondManager(EQL_CONFIG_GROUP_NAME, connectionName);
            val eqlConfig = new DefaultEqlConfigDecorator(this);
            diamondListener = diamondStone -> EqlConfigManager.invalidateCache(eqlConfig);
            diamondManager.addDiamondListener(diamondListener);
        }

        @Override
        public void onRemoval() {
            diamondManager.removeDiamondListener(diamondListener);
        }
    }
}
