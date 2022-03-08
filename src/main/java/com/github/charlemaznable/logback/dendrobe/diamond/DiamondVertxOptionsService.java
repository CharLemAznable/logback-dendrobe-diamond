package com.github.charlemaznable.logback.dendrobe.diamond;

import com.github.charlemaznable.logback.dendrobe.impl.DefaultVertxOptionsService;
import com.github.charlemaznable.logback.dendrobe.vertx.VertxOptionsService;
import com.github.charlemaznable.vertx.config.VertxOptionsConfigElf;
import com.google.auto.service.AutoService;

@AutoService(VertxOptionsService.class)
public final class DiamondVertxOptionsService extends DefaultVertxOptionsService {

    @Override
    public String getVertxOptionsValue(String configKey) {
        return VertxOptionsConfigElf.getDiamondStone(configKey);
    }
}
