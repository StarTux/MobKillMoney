package com.cavetale.mobkillmoney;

import lombok.NonNull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;

enum SpecialMob {
    SPIDER_JOCKEY,
    CHICKEN_JOCKEY;

    static SpecialMob of(@NonNull LivingEntity entity) {
        switch (entity.getType()) {
        case SKELETON: {
            final Skeleton skele = (Skeleton) entity;
            if (skele.getVehicle() == null) return null;
            if (skele.getVehicle().getType() != EntityType.SPIDER) return null;
            return SpecialMob.SPIDER_JOCKEY;
        }
        case ZOMBIE: {
            final Zombie zombie = (Zombie) entity;
            if (!zombie.isBaby()) return null;
            if (zombie.getVehicle() == null) return null;
            if (zombie.getVehicle().getType() != EntityType.CHICKEN) {
                return null;
            }
            return SpecialMob.CHICKEN_JOCKEY;
        }
        default:
            return null;
        }
    }
}
