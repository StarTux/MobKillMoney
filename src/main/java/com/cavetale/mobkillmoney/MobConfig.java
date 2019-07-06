package com.cavetale.mobkillmoney;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.ToString;

@ToString
final class MobConfig {
    Enum type;
    String displayName;
    double killReward;
    double deathCost;

    MobConfig(final Enum type) {
        this.type = type;
        displayName = Stream.of(type.name().split("_"))
            .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
}
