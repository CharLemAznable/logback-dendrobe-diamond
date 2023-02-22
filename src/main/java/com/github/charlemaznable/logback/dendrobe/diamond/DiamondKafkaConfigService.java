package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.core.kafka.KafkaConfigElf;
import com.github.charlemaznable.logback.dendrobe.impl.DefaultKafkaConfigService;
import com.github.charlemaznable.logback.dendrobe.kafka.KafkaConfigService;
import com.google.auto.service.AutoService;

@AutoService(KafkaConfigService.class)
public class DiamondKafkaConfigService extends DefaultKafkaConfigService {

    @Override
    public String getKafkaConfigValue(String configKey) {
        return KafkaConfigElf.getDiamondStone(configKey);
    }
}
