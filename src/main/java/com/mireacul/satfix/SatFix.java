package com.mireacul.satfix;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * SatFix — Mireacul UHC
 *
 * Problem: When naturalRegeneration is handled by Skript (cancelling the heal event),
 * the NMS FoodMetaData tick still adds 3.0 exhaustion every 80 ticks for healing attempts.
 * Players below full health drain saturation far faster than players at full health.
 *
 * Fix: Reset the internal foodTickTimer to 0 every tick so the healing exhaustion
 * never accumulates. Also cancel SATIATED heals as a safety net.
 * Result: Saturation depletes at the same rate regardless of health.
 */
public class SatFix extends JavaPlugin implements Listener {

    private Method getHandleMethod;
    private Field foodDataField;
    private Field foodTickTimerField;
    private boolean active = true;

    @Override
    public void onEnable() {
        if (!hookNMS()) {
            getLogger().severe("Failed to hook NMS — SatFix disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) return;
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (Player player : world.getPlayers()) {
                        resetHealTimer(player);
                    }
                }
            }
        }.runTaskTimer(this, 1L, 1L);

        getLogger().info("SatFix enabled — saturation normalized.");
    }

    private boolean hookNMS() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            getHandleMethod = craftPlayer.getMethod("getHandle");

            Class<?> entityHuman = Class.forName("net.minecraft.server." + version + ".EntityHuman");
            foodDataField = entityHuman.getDeclaredField("foodData");
            foodDataField.setAccessible(true);

            Class<?> foodMeta = Class.forName("net.minecraft.server." + version + ".FoodMetaData");
            foodTickTimerField = foodMeta.getDeclaredField("foodTickTimer");
            foodTickTimerField.setAccessible(true);

            getLogger().info("Hooked NMS version: " + version);
            return true;
        } catch (Exception e) {
            getLogger().severe("NMS hook failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void resetHealTimer(Player player) {
        try {
            Object entityPlayer = getHandleMethod.invoke(player);
            Object foodData = foodDataField.get(entityPlayer);
            foodTickTimerField.setInt(foodData, 0);
        } catch (Exception ignored) {
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getRegainReason() == RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDisable() {
        active = false;
        getLogger().info("SatFix disabled.");
    }
}
