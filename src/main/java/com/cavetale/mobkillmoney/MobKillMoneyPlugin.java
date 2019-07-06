package com.cavetale.mobkillmoney;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobKillMoneyPlugin extends JavaPlugin implements Listener {
    VaultAPI vaultAPI = new VaultAPI(this);
    Map<EntityType, MobConfig> entityTypes;
    Map<SpecialMob, MobConfig> specialMobs;
    String killMessage;
    String deathMessage;
    int killLimit;
    Map<Chonk.Loc, Chonk> chonks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        if (!vaultAPI.setup()) {
            getLogger().warning("Vault economy missing!");
        }
        loadAllConfigs();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler()
            .runTaskTimer(this, this::everyMinute, 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        vaultAPI = null;
        entityTypes = null;
        specialMobs = null;
        killMessage = null;
        deathMessage = null;
        chonks = null;
    }

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "reload":
            if (args.length != 1) return false;
            reloadConfig();
            loadAllConfigs();
            sender.sendMessage("MobKillMoney config reloaded.");
            return true;
        case "info":
            sender.sendMessage("MobKillMoney version 0.1");
            sender.sendMessage("killMessage: " + killMessage);
            sender.sendMessage("deathMessage: " + deathMessage);
            sender.sendMessage("killLimit: " + killLimit);
            return true;
        case "chunk":
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Chonk chonk = chonkAt(player.getLocation());
                player.sendMessage("Chunk " + chonk.loc.world + "/"
                                   + chonk.loc.x + "," + chonk.loc.z
                                   + ": " + chonk.kills + " kills.");
            } else {
                sender.sendMessage("Player expected.");
            }
            return true;
        default:
            return false;
        }
    }

    void everyMinute() {
        for (Iterator<Map.Entry<Chonk.Loc, Chonk>> it =
                 chonks.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Chonk.Loc, Chonk> entry = it.next();
            Chonk chonk = entry.getValue();
            chonk.kills -= 1;
            if (chonk.kills < 0) it.remove();
        }
    }

    /**
     * Fetch MobConfig of either special mob or entity type.
     * @return The MobConfig, never null.
     */
    MobConfig mobConfigOf(@NonNull LivingEntity entity) {
        final SpecialMob specialMob = SpecialMob.of(entity);
        if (specialMob != null) return mobConfigOf(specialMob);
        return mobConfigOf(entity.getType());
    }

    MobConfig mobConfigOf(@NonNull SpecialMob specialMob) {
        MobConfig result = specialMobs.get(specialMob);
        if (result == null) {
            result = new MobConfig(specialMob);
            specialMobs.put(specialMob, result);
        }
        return result;
    }

    MobConfig mobConfigOf(@NonNull EntityType entityType) {
        MobConfig result = entityTypes.get(entityType);
        if (result == null) {
            result = new MobConfig(entityType);
            entityTypes.put(entityType, result);
        }
        return result;
    }

    Chonk chonkAt(@NonNull Chonk.Loc loc) {
        Chonk chonk = chonks.get(loc);
        if (chonk == null) {
            chonk = new Chonk(loc);
            chonks.put(loc, chonk);
        }
        return chonk;
    }

    Chonk chonkAt(@NonNull Location location) {
        Chunk chunk = location.getChunk();
        Chonk.Loc loc = new Chonk.Loc(chunk.getWorld().getName(),
                                      chunk.getX(), chunk.getZ());
        return chonkAt(loc);
    }

    Chonk mobKillAt(@NonNull Location location) {
        Chonk chonk = chonkAt(location);
        for (int z = -1; z <= 1; z += 1) {
            for (int x = -1; x <= 1; x += 1) {
                chonkAt(chonk.loc.relative(x, z)).kills += 1;
            }
        }
        return chonk;
    }

    void loadAllConfigs() {
        entityTypes = new EnumMap<>(EntityType.class);
        specialMobs = new EnumMap<>(SpecialMob.class);
        ConfigurationSection sec = getConfig()
            .getConfigurationSection("killReward");
        for (String key : sec.getKeys(false)) {
            try {
                SpecialMob specialMob = SpecialMob.valueOf(key.toUpperCase());
                mobConfigOf(specialMob).killReward = sec.getDouble(key);
                continue;
            } catch (IllegalArgumentException iae) { }
            try {
                EntityType entityType = EntityType.valueOf(key.toUpperCase());
                mobConfigOf(entityType).killReward = sec.getDouble(key);
            } catch (IllegalArgumentException iae) {
                getLogger().warning("killRewards." + key + ": Unknown mob!");
            }
        }
        sec = getConfig().getConfigurationSection("deathCost");
        for (String key : sec.getKeys(false)) {
            try {
                SpecialMob specialMob = SpecialMob.valueOf(key.toUpperCase());
                mobConfigOf(specialMob).deathCost = sec.getDouble(key);
                continue;
            } catch (IllegalArgumentException iae) { }
            try {
                EntityType entityType = EntityType.valueOf(key.toUpperCase());
                mobConfigOf(entityType).deathCost = sec.getDouble(key);
            } catch (IllegalArgumentException iae) {
                getLogger().warning("deathCosts." + key + ": Unknown mob!");
            }
        }
        killMessage = getConfig().getString("killMessage", "");
        killMessage = ChatColor.translateAlternateColorCodes('&', killMessage);
        deathMessage = getConfig().getString("deathMessage", "");
        deathMessage = ChatColor
            .translateAlternateColorCodes('&', deathMessage);
        killLimit = getConfig().getInt("killLimit");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onEntityDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final Player player = entity.getKiller();
        if (player == null) return;
        // Check chonk
        if (mobKillAt(entity.getLocation()).kills > killLimit) return;
        // Give money and notify
        MobConfig mobConfig = mobConfigOf(entity);
        final double money = mobConfig.killReward;
        if (money < 0.01) return;
        vaultAPI.give(player, money);
        notify(player, killMessage, mobConfig, money);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (!(player.getLastDamageCause()
              instanceof EntityDamageByEntityEvent)) {
            return;
        }
        final EntityDamageByEntityEvent damageEvent =
            (EntityDamageByEntityEvent) player.getLastDamageCause();
        // Find killer
        final Entity damager = damageEvent.getDamager();
        final LivingEntity killer;
        if (damager instanceof LivingEntity) {
            killer = (LivingEntity) damager;
        } else if (damager instanceof Projectile) {
            final Projectile projectile = (Projectile) damager;
            if (!(projectile.getShooter() instanceof LivingEntity)) return;
            killer = (LivingEntity) projectile.getShooter();
        } else {
            return;
        }
        // Take money and notify
        MobConfig mobConfig = mobConfigOf(killer);
        final double money = mobConfig.killReward;
        if (money < 0.01) return;
        vaultAPI.take(player, money);
        notify(player, deathMessage, mobConfig, money);
    }

    void notify(@NonNull Player player,
                String message,
                @NonNull MobConfig mobConfig,
                final double money) {
        if (message == null || message.isEmpty()) return;
        message = message
            .replace("{mob}", mobConfig.displayName)
            .replace("{money}", vaultAPI.format(money));
        if (player.isDead()) {
            player.sendMessage(message);
        } else {
            player.sendActionBar(message);
        }
    }
}
