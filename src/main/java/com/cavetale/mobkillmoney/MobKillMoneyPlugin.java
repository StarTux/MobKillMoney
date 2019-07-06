package com.cavetale.mobkillmoney;

import java.util.EnumMap;
import java.util.Map;
import lombok.NonNull;
import org.bukkit.ChatColor;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobKillMoneyPlugin extends JavaPlugin implements Listener {
    VaultAPI vaultAPI = new VaultAPI(this);
    Map<EntityType, MobConfig> entityTypes;
    Map<SpecialMob, MobConfig> specialMobs;
    String killMessage;
    String deathMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        if (!vaultAPI.setup()) {
            getLogger().warning("Vault economy missing!");
        }
        loadAllConfigs();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        vaultAPI = null;
        entityTypes = null;
        specialMobs = null;
        killMessage = null;
        deathMessage = null;
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
        }
        return true;
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

    MobConfig mobConfigOf(SpecialMob specialMob) {
        MobConfig result = specialMobs.get(specialMob);
        if (result == null) {
            result = new MobConfig(specialMob);
            specialMobs.put(specialMob, result);
        }
        return result;
    }

    MobConfig mobConfigOf(EntityType entityType) {
        MobConfig result = entityTypes.get(entityType);
        if (result == null) {
            result = new MobConfig(entityType);
            entityTypes.put(entityType, result);
        }
        return result;
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
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onEntityDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final Player player = entity.getKiller();
        if (player == null) return;
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
              instanceof EntityDamageByEntityEvent)) return;
        final EntityDamageByEntityEvent damageEvent =
            (EntityDamageByEntityEvent) player.getLastDamageCause();
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
