package com.cavetale.mobkillmoney;

import org.junit.Assert;
import org.junit.Test;
import org.bukkit.entity.EntityType;

public final class MobKillMoneyTest {
    @Test
    public void test() {
        for (EntityType et : EntityType.values()) {
            if (!et.isAlive()) continue;
            if (org.bukkit.entity.Animals.class.isAssignableFrom(et.getEntityClass())) continue;
            System.out.println("    " + et.name().toLowerCase() + ": 1");
        }
    }
}
