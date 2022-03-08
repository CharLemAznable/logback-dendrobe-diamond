package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.es.config.EsConfigElf;
import com.github.charlemaznable.logback.dendrobe.es.EsConfigService;
import com.github.charlemaznable.logback.dendrobe.impl.DefaultEsConfigService;
import com.google.auto.service.AutoService;

@AutoService(EsConfigService.class)
public final class DiamondEsConfigService extends DefaultEsConfigService {

    @Override
    public String getEsConfigValue(String configKey) {
        return EsConfigElf.getDiamondStone(configKey);
    }
}
