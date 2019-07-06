package com.cavetale.mobkillmoney;

import lombok.NonNull;
import lombok.Value;

/**
 * A namespace-dodging chunk.
 */
final class Chonk {
    Loc loc;
    int kills;

    Chonk(@NonNull final Loc loc) {
        this.loc = loc;
        this.kills = 0;
    }

    @Value
    static class Loc {
        public final String world;
        public final int x;
        public final int z;

        Loc relative(final int dx, final int dz) {
            return new Loc(world, x + dx, z + dz);
        }
    }
}
