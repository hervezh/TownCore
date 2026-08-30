package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WarManager {

    private static final List<War> wars = new ArrayList<>();
    private static final Map<String, Integer> coreHealthPercent = new HashMap<>();
    private static final Map<String, String> occupiedBy = new HashMap<>(); // defenderTown -> attackerTown
    private static final Map<String, Location> originalCoreLocations = new HashMap<>();
    private static final Map<String, Integer> teleportTasks = new HashMap<>();
    private static final Map<String, Long> lastDamageTime = new HashMap<>();
    private static final Map<String, Long> lastRegenTime = new HashMap<>();
    private static final Map<String, Long> lastAlarmTime = new HashMap<>();

    private static final int AUTO_SESSION_MINUTES = 20;
    private static final long AUTO_WAIT_MS = 60L * 60L * 1000L; // 1 hour
    private static long lastSessionEndTime = 0L;

    private static boolean automaticWarSessionsEnabled = false;

    public static class War {
        public String attackerTown;
        public String defenderTown;
        public int attackerPoints;
        public int defenderPoints;
        public boolean activeSession;
        public long sessionEndTime;
        public java.util.Set<String> attackerAllies;
        public java.util.Set<String> defenderAllies;
        public boolean isRevolt;

        public War(String attackerTown, String defenderTown) {
            this.attackerTown = attackerTown;
            this.defenderTown = defenderTown;
            this.attackerPoints = 0;
            this.defenderPoints = 0;
            this.activeSession = false;
            this.sessionEndTime = 0L;
            this.attackerAllies = new java.util.HashSet<>();
            this.defenderAllies = new java.util.HashSet<>();
            this.isRevolt = false;
        }
    }

    private static final long SHIELD_DURATION_MS = 3 * 24 * 60 * 60 * 1000L;

    public static void declareWar(Player player, String attackerTown, String defenderTown) {
        if (attackerTown == null || defenderTown == null) return;

        // check if the defender has a peace shield active
        if (Main.townShieldExpiry.getOrDefault(defenderTown, 0L) > System.currentTimeMillis()) {
            long remaining = Main.townShieldExpiry.get(defenderTown) - System.currentTimeMillis();
            long hours = remaining / 3600000;
            long minutes = (remaining % 3600000) / 60000;
            player.sendMessage("§c§l[Town Shield] §r§fThat town is currently under a peace shield! §7(§f" + hours + "h " + minutes + "m§7 remaining)");
            return;
        }

        // attacker also can't declare war while shielded
        if (Main.townShieldExpiry.getOrDefault(attackerTown, 0L) > System.currentTimeMillis()) {
            long remaining = Main.townShieldExpiry.get(attackerTown) - System.currentTimeMillis();
            long hours = remaining / 3600000;
            player.sendMessage("§c§l[Town Shield] §r§fYour town is under a peace shield and cannot declare war! §7(§f" + hours + "h§7 remaining)");
            return;
        }

        if (getWarByTown(attackerTown) != null) {
            if (player != null) {
                player.sendMessage("§cYour town is already in a war!");
            }
            return;
        }

        if (getWarByTown(defenderTown) != null) {
            if (player != null) {
                player.sendMessage("§cThat town is already in a war!");
            }
            return;
        }

        War war = new War(attackerTown, defenderTown);
        wars.add(war);

        // make sure both sides track each other in the war map
        Main.townWars.putIfAbsent(attackerTown, new HashSet<>());
        Main.townWars.putIfAbsent(defenderTown, new HashSet<>());

        Main.townWars.get(attackerTown).add(defenderTown);
        Main.townWars.get(defenderTown).add(attackerTown);

        coreHealthPercent.put(defenderTown, 100);

        Bukkit.broadcastMessage("§4§l[WAR] §r§f" + attackerTown
                + " §chas declared war on §f" + defenderTown + "§c!");
        Bukkit.broadcastMessage("§7Staff can start the battle using §f/town adminstartwar " + attackerTown + " <minutes>");
    }

    public static List<War> getAllWars() {
        return new ArrayList<>(wars);
    }

    public static War getWarByTown(String townName) {
        if (townName == null) return null;

        for (War war : wars) {
            if (war.attackerTown.equalsIgnoreCase(townName)
                    || war.defenderTown.equalsIgnoreCase(townName)
                    || war.attackerAllies.contains(townName)
                    || war.defenderAllies.contains(townName)) {
                return war;
            }
        }

        return null;
    }

    public static War getWarByDefenderTown(String townName) {
        if (townName == null) return null;

        for (War war : wars) {
            if (war.defenderTown.equalsIgnoreCase(townName)) {
                return war;
            }
        }

        return null;
    }

    public static War getWarById(String warId) {
        if (warId == null) return null;

        for (War war : wars) {
            if (getWarId(war).equalsIgnoreCase(warId)) {
                return war;
            }
        }

        return null;
    }

    public static String getWarId(War war) {
        if (war == null) return "unknown";

        return getWarKey(war.attackerTown, war.defenderTown);
    }

    public static String getWarKey(String attackerTown, String defenderTown) {
        if (attackerTown == null || defenderTown == null) return "unknown";

        String attacker = attackerTown.replace(" ", "_").toLowerCase();
        String defender = defenderTown.replace(" ", "_").toLowerCase();

        return attacker + "_vs_" + defender;
    }

    public static boolean hasActiveWarSession() {
        for (War war : wars) {
            if (war.activeSession) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPlayerInActiveWarSession(Player player) {
        if (player == null) return false;

        String townName = Main.playerTown.get(player.getUniqueId());

        if (townName == null) return false;

        War war = getWarByTown(townName);

        return war != null && war.activeSession;
    }

    public static boolean forceStartSession(String townName, int minutes, String staffName) {
        War war = getWarByTown(townName);

        if (war == null) return false;
        if (war.activeSession) return false;
        if (hasActiveWarSession()) return false;

        war.activeSession = true;
        war.sessionEndTime = System.currentTimeMillis() + (minutes * 60L * 1000L);

        coreHealthPercent.put(war.defenderTown, 100);

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(WarManager.class);
        WarBossBarManager.startWarBossBar(plugin, war, minutes * 60);

        Bukkit.broadcastMessage("§4§l[WAR] §r§cA war session has started!");
        Bukkit.broadcastMessage("§cWar: §f" + war.attackerTown + " §7vs §f" + war.defenderTown);
        Bukkit.broadcastMessage("§cStarted by: §f" + staffName);
        Bukkit.broadcastMessage("§cDuration: §f" + minutes + " minutes");
        Bukkit.broadcastMessage("§7Attackers must damage the defender Town Core.");
 
        playGlobalWarHorn();
        startTeleportTask(war);

        return true;
    }

    public static boolean forceEndSessionById(String warId, String staffName) {
        War war = getWarById(warId);

        if (war == null) return false;
        if (!war.activeSession) return false;

        war.activeSession = false;
        war.sessionEndTime = 0L;

        WarBossBarManager.stopWarBossBar(getWarId(war));

        Bukkit.broadcastMessage("§4§l[WAR] §r§cThe active war session has ended.");
        Bukkit.broadcastMessage("§cWar: §f" + war.attackerTown + " §7vs §f" + war.defenderTown);
        Bukkit.broadcastMessage("§cEnded by: §f" + staffName);
        Bukkit.broadcastMessage("§cScore: §f" + war.attackerTown + " " + war.attackerPoints
                + " §7- §f" + war.defenderPoints + " " + war.defenderTown);

        String id = getWarId(war);
        wars.remove(war);

        long expiry = System.currentTimeMillis() + SHIELD_DURATION_MS;
        Main.townShieldExpiry.put(war.defenderTown, expiry);

        Main.townUpgrades.getOrDefault(war.attackerTown, new HashSet<>()).remove(TownUpgradesManager.PERK_REINFORCED_CORE);
        Main.townUpgrades.getOrDefault(war.defenderTown, new HashSet<>()).remove(TownUpgradesManager.PERK_REINFORCED_CORE);

        restoreCore(war);

        return true;
    }

    public static boolean terminateWarById(String warId, String staffName) {
        War war = getWarById(warId);

        if (war == null) return false;

        Bukkit.broadcastMessage("§4§l[WAR] §r§cWar terminated by staff.");
        Bukkit.broadcastMessage("§cWar: §f" + war.attackerTown + " §7vs §f" + war.defenderTown);
        Bukkit.broadcastMessage("§cTerminated by: §f" + staffName);

        removeWar(war);
        return true;
    }

    public static void damageCore(Player player, String defenderTown, int damage) {
        if (player == null || defenderTown == null || damage <= 0) return;

        War war = getWarByDefenderTown(defenderTown);

        if (war == null || !war.activeSession) return;

        String playerTown = Main.playerTown.get(player.getUniqueId());

        if (playerTown == null) {
            player.sendMessage("§cYou must be in a town to damage this Town Core!");
            return;
        }

        if (!playerTown.equalsIgnoreCase(war.attackerTown) && !war.attackerAllies.contains(playerTown)) {
            player.sendMessage("§cOnly attackers and their allies can damage the defender Town Core!");
            return;
        }

        int currentHealth = coreHealthPercent.getOrDefault(defenderTown, 100);
        int newHealth = Math.max(0, currentHealth - damage);

        coreHealthPercent.put(defenderTown, newHealth);
        lastDamageTime.put(defenderTown, System.currentTimeMillis());
        triggerAlarm(defenderTown);

        if (newHealth > 0) {
            return;
        }

        war.attackerPoints++;
        coreHealthPercent.put(defenderTown, 100);

        Bukkit.broadcastMessage("§4§l[WAR] §r§f" + war.attackerTown
                + " §chas destroyed §f" + war.defenderTown + "§c's Town Core!");
        Bukkit.broadcastMessage("§cScore: §f" + war.attackerTown + " " + war.attackerPoints
                + " §7- §f" + war.defenderPoints + " " + war.defenderTown);

        Location core = Main.townCoreLocation.get(defenderTown);

        if (core != null && core.getWorld() != null) {
            core.getBlock().setType(Material.BEACON);
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(WarManager.class);
        StorageManager.saveData(plugin);
    }

    public static void addDefenderPoint(String defenderTown) {
        War war = getWarByDefenderTown(defenderTown);

        if (war == null) return;

        war.defenderPoints++;

        Bukkit.broadcastMessage("§4§l[WAR] §r§f" + defenderTown + " §ahas earned a defender point!");
        Bukkit.broadcastMessage("§cScore: §f" + war.attackerTown + " " + war.attackerPoints
                + " §7- §f" + war.defenderPoints + " " + war.defenderTown);
    }

    public static void removeWar(War war) {
        if (war == null) return;

        war.activeSession = false;
        war.sessionEndTime = 0L;

        WarBossBarManager.stopWarBossBar(getWarId(war));

        long expiry = System.currentTimeMillis() + SHIELD_DURATION_MS;
        Main.townShieldExpiry.put(war.defenderTown, expiry);

        wars.remove(war);

        if (Main.townWars.containsKey(war.attackerTown)) {
            Main.townWars.get(war.attackerTown).remove(war.defenderTown);
        }

        if (Main.townWars.containsKey(war.defenderTown)) {
            Main.townWars.get(war.defenderTown).remove(war.attackerTown);
        }

        coreHealthPercent.remove(war.defenderTown);
    }

    public static int getCoreHealthPercent(String defenderTown) {
        if (defenderTown == null) return 100;

        return Math.max(0, Math.min(100, coreHealthPercent.getOrDefault(defenderTown, 100)));
    }

    public static void setCoreHealthPercent(String town, int health) {
        coreHealthPercent.put(town, health);
    }

    public static Location getOriginalCoreLocation(String town) {
        return originalCoreLocations.get(town);
    }

    public static Map<String, Location> getOriginalCoreLocations() {
        return originalCoreLocations;
    }

    public static boolean isAutomaticWarSessionsEnabled() {
        return automaticWarSessionsEnabled;
    }

    public static void setAutomaticWarSessionsEnabled(boolean enabled) {
        automaticWarSessionsEnabled = enabled;
    }

    public static void startAutomaticWarScheduler(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!automaticWarSessionsEnabled) return;
            if (hasActiveWarSession()) return;
            if (wars.isEmpty()) return;

            // Only run on weekends
            java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
            if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY) return;

            long now = System.currentTimeMillis();
            if (now - lastSessionEndTime < AUTO_WAIT_MS && lastSessionEndTime > 0) return;

            for (War war : wars) {
                if (!war.activeSession) {
                    war.activeSession = true;
                    war.sessionEndTime = now + (AUTO_SESSION_MINUTES * 60L * 1000L);
                    coreHealthPercent.put(war.defenderTown, 100);

                    WarBossBarManager.startWarBossBar(plugin, war, AUTO_SESSION_MINUTES * 60);

                    Bukkit.broadcastMessage("§4§l[WAR] §r§cAn automatic war session has started!");
                    Bukkit.broadcastMessage("§cWar: §f" + war.attackerTown + " §7vs §f" + war.defenderTown);
                    Bukkit.broadcastMessage("§cDuration: §f" + AUTO_SESSION_MINUTES + " minutes");
                    Bukkit.broadcastMessage("§7Attackers must damage the defender's Town Core.");
                    playGlobalWarHorn();
                    break;
                }
            }
        }, 20L * 60, 20L * 60); // Check every 60 seconds
    }

    public static void shutdownWarSystem() {
        for (War war : wars) {
            war.activeSession = false;
            war.sessionEndTime = 0L;
        }

        WarBossBarManager.stopAllWarBossBars();
    }

    public static void startWarTicker(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            tickWarSessions();
            tickCoreRegeneration();
            tickCoreVisuals();
        }, 20L, 20L);
    }

    public static void tickWarSessions() {
        long now = System.currentTimeMillis();

        for (War war : wars) {
            if (war.activeSession && war.sessionEndTime > 0 && now >= war.sessionEndTime) {
                war.activeSession = false;
                war.sessionEndTime = 0L;
                lastSessionEndTime = now;

                WarBossBarManager.stopWarBossBar(getWarId(war));

                Bukkit.broadcastMessage("§4§l[WAR] §r§cWar session has ended!");
                Bukkit.broadcastMessage("§cWar: §f" + war.attackerTown + " §7vs §f" + war.defenderTown);
                Bukkit.broadcastMessage("§cScore: §f" + war.attackerTown + " " + war.attackerPoints
                        + " §7- §f" + war.defenderPoints + " " + war.defenderTown);

                // Check if attacker won this session
                if (war.attackerPoints > war.defenderPoints) {
                    // Check if this was a revolt (attacker was occupied by defender)
                    if (isOccupied(war.attackerTown) && getOccupier(war.attackerTown).equalsIgnoreCase(war.defenderTown)) {
                        freeOccupation(war.attackerTown);
                    } else {
                        applyOccupation(war.attackerTown, war.defenderTown);
                    }
                } else if (war.defenderPoints > war.attackerPoints) {
                    Bukkit.broadcastMessage("§a§l[WAR] §f" + war.defenderTown + " §asuccessfully defended!");

                    // Free from occupation if the defender was occupied by this attacker
                    if (isOccupied(war.defenderTown)
                            && war.attackerTown.equalsIgnoreCase(getOccupier(war.defenderTown))) {
                        freeOccupation(war.defenderTown);
                    }
                } else {
                    Bukkit.broadcastMessage("§e§l[WAR] §7The session ended in a draw.");
                }

                // Clear temporary perks
                Main.townUpgrades.getOrDefault(war.attackerTown, new HashSet<>()).remove(TownUpgradesManager.PERK_REINFORCED_CORE);
                Main.townUpgrades.getOrDefault(war.defenderTown, new HashSet<>()).remove(TownUpgradesManager.PERK_REINFORCED_CORE);

                restoreCore(war);
                lastDamageTime.remove(war.defenderTown);
                lastRegenTime.remove(war.defenderTown);
            }
        }
    }

    private static void tickCoreVisuals() {
        for (War war : wars) {
            if (!war.activeSession) continue;

            String defender = war.defenderTown;
            Location core = Main.townCoreLocation.get(defender);
            if (core == null || core.getWorld() == null) continue;

            int health = getCoreHealthPercent(defender);
            org.bukkit.World world = core.getWorld();
            Location center = core.clone().add(0.5, 0.5, 0.5);

            if (health >= 70) {
                // Green Dust
                world.spawnParticle(org.bukkit.Particle.DUST, center, 10, 0.4, 0.4, 0.4, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 0), 1.2f));
                world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, center, 2, 0.4, 0.4, 0.4, 0.02);
            } else if (health >= 30) {
                // Yellow Dust
                world.spawnParticle(org.bukkit.Particle.DUST, center, 12, 0.4, 0.4, 0.4, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 0), 1.2f));
                world.spawnParticle(org.bukkit.Particle.CRIT, center, 4, 0.4, 0.4, 0.4, 0.05);
            } else {
                // Red Dust & Smoke
                world.spawnParticle(org.bukkit.Particle.DUST, center, 15, 0.3, 0.3, 0.3, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 1.5f));
                world.spawnParticle(org.bukkit.Particle.FLAME, center, 8, 0.3, 0.3, 0.3, 0.05);
                world.spawnParticle(org.bukkit.Particle.SMOKE, center, 15, 0.3, 0.6, 0.3, 0.02);
                if (Math.random() < 0.2) {
                    world.spawnParticle(org.bukkit.Particle.LAVA, center, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
    }

    private static void triggerAlarm(String defenderTown) {
        long now = System.currentTimeMillis();
        long lastAlarm = lastAlarmTime.getOrDefault(defenderTown, 0L);
        long alarmInterval = 20L * 1000L; // 20 seconds between audible alarms

        if (now - lastAlarm < alarmInterval) return;

        lastAlarmTime.put(defenderTown, now);

        for (UUID uuid : Main.townMembers.getOrDefault(defenderTown, new java.util.HashSet<>())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§4§l🚨 ATTACK! §cYour Town Core is being damaged!");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.8f);
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            }
        }
    }

    private static void tickCoreRegeneration() {
        long now = System.currentTimeMillis();
        long idleThreshold = 90L * 1000L; // 1 minute 30 seconds
        long regenInterval = 20L * 1000L; // 20 seconds

        for (War war : wars) {
            if (!war.activeSession) continue;

            String defender = war.defenderTown;
            int health = getCoreHealthPercent(defender);
            if (health >= 100) continue;

            long lastDamage = lastDamageTime.getOrDefault(defender, 0L);
            if (now - lastDamage < idleThreshold) continue;

            long lastRegen = lastRegenTime.getOrDefault(defender, 0L);
            if (now - lastRegen < regenInterval) {
                continue;
            }

            // Regenerate 1%
            int newHealth = Math.min(100, health + 1);
            coreHealthPercent.put(defender, newHealth);
            lastRegenTime.put(defender, now);
        }
    }

    private static void applyOccupation(String attackerTown, String defenderTown) {
        occupiedBy.put(defenderTown, attackerTown);

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§4§l⚑ OCCUPATION §r§c" + defenderTown + " §7is now occupied by §f" + attackerTown + "§7!");
        Bukkit.broadcastMessage("§7All of §f" + defenderTown + "§7's income now goes to §f" + attackerTown + "§7.");
        Bukkit.broadcastMessage(" ");

        // Notify defender members
        for (UUID uuid : Main.townMembers.getOrDefault(defenderTown, new HashSet<>())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§c§lYour town has been occupied! Your income now goes to §f" + attackerTown + "§c.");
            }
        }

        // Notify attacker members
        for (UUID uuid : Main.townMembers.getOrDefault(attackerTown, new HashSet<>())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§a§lYou now control §f" + defenderTown + "§a's income!");
            }
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(WarManager.class);
        StorageManager.saveData(plugin);
    }

    public static boolean isOccupied(String townName) {
        return occupiedBy.containsKey(townName);
    }

    public static String getOccupier(String townName) {
        return occupiedBy.get(townName);
    }

    public static void freeOccupation(String townName) {
        String occupier = occupiedBy.remove(townName);
        if (occupier != null) {
            Bukkit.broadcastMessage("§a§l⚑ LIBERATION §f" + townName + " §ais no longer occupied by §f" + occupier + "§a!");

            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(WarManager.class);
            StorageManager.saveData(plugin);
        }
    }

    public static Map<String, String> getOccupiedTowns() {
        return new HashMap<>(occupiedBy);
    }

    public static void setOccupiedTowns(Map<String, String> data) {
        occupiedBy.clear();
        if (data != null) {
            occupiedBy.putAll(data);
        }
    }

    public static void revolt(Player player, String townName) {
        if (!isOccupied(townName)) {
            player.sendMessage("§cYour town is not occupied!");
            return;
        }

        if (Main.townShieldExpiry.getOrDefault(townName, 0L) > System.currentTimeMillis()) {
            long remaining = Main.townShieldExpiry.get(townName) - System.currentTimeMillis();
            long hours = remaining / 3600000;
            player.sendMessage("§c§l[Town Shield] §r§fYour town is still recovering and cannot revolt yet! §7(§f" + hours + "h§7 remaining)");
            return;
        }

        String occupier = getOccupier(townName);
        if (occupier == null) return;

        if (getWarByTown(townName) != null) {
            player.sendMessage("§cYour town is already in a war!");
            return;
        }

        if (getWarByTown(occupier) != null) {
            player.sendMessage("§cThe occupier town is already in a war!");
            return;
        }

        // A revolt is a war where the revolting town is the attacker
        War war = new War(townName, occupier);
        war.isRevolt = true;
        wars.add(war);

        Main.townWars.putIfAbsent(townName, new HashSet<>());
        Main.townWars.get(townName).add(occupier);
        Main.townWars.putIfAbsent(occupier, new HashSet<>());
        Main.townWars.get(occupier).add(townName);

        coreHealthPercent.put(occupier, 100);

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§4§l⚔ REVOLT ⚔");
        Bukkit.broadcastMessage("§f" + townName + " §chas risen against §f" + occupier + "§c!");
        Bukkit.broadcastMessage("§7Victory is decided by §fKills §7inside §f" + townName + "§7's territory!");
        Bukkit.broadcastMessage(" ");
        
        playGlobalWarHorn();
    }

    public static void playGlobalWarHorn() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            p.playSound(p.getLocation(), org.bukkit.Sound.EVENT_RAID_HORN, 1.0f, 0.8f);
        }
    }

    public static void cleanupInvalidWars() {
        Iterator<War> iterator = wars.iterator();

        while (iterator.hasNext()) {
            War war = iterator.next();

            if (!Main.townLevel.containsKey(war.attackerTown)
                    || !Main.townLevel.containsKey(war.defenderTown)) {

                WarBossBarManager.stopWarBossBar(getWarId(war));

                if (Main.townWars.containsKey(war.attackerTown)) {
                    Main.townWars.get(war.attackerTown).remove(war.defenderTown);
                }

                if (Main.townWars.containsKey(war.defenderTown)) {
                    Main.townWars.get(war.defenderTown).remove(war.attackerTown);
                }

                coreHealthPercent.remove(war.defenderTown);
                iterator.remove();
            }
        }
    }

    private static void startTeleportTask(War war) {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(WarManager.class);
        String defender = war.defenderTown;
        Location original = Main.townCoreLocation.get(defender);
        if (original == null) return;

        originalCoreLocations.put(defender, original.clone());

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!war.activeSession) return;
            
            teleportCore(war);
        }, 20L * 60 * 5, 20L * 60 * 5).getTaskId();

        teleportTasks.put(defender, taskId);
    }

    private static void teleportCore(War war) {
        String defender = war.defenderTown;
        Location current = Main.townCoreLocation.get(defender);
        Location original = originalCoreLocations.get(defender);
        if (current == null || original == null) return;

        List<String> chunks = new ArrayList<>(Main.townChunks.getOrDefault(defender, new HashSet<>()));
        if (chunks.isEmpty()) return;

        java.util.Collections.shuffle(chunks);
        for (String chunkKey : chunks) {
            String[] split = chunkKey.split(":");
            if (split.length != 2) continue;
            org.bukkit.World world = Bukkit.getWorld(split[0]);
            if (world == null) continue;

            String[] coords = split[1].split(",");
            int cx = Integer.parseInt(coords[0]);
            int cz = Integer.parseInt(coords[1]);

            // Try to find a spot at similar Y
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int bx = (cx << 4) + x;
                    int bz = (cz << 4) + z;
                    
                    for (int dy = -3; dy <= 3; dy++) {
                        int by = original.getBlockY() + dy;
                        if (by < world.getMinHeight() || by >= world.getMaxHeight()) continue;

                        org.bukkit.block.Block b = world.getBlockAt(bx, by, bz);
                        if (b.getType().isAir() && b.getRelative(0, -1, 0).getType().isSolid()) {
                            // Found a spot!
                            // Remove old beacon
                            current.getBlock().setType(Material.AIR);
                            
                            // Set new beacon
                            Location next = b.getLocation();
                            next.getBlock().setType(Material.BEACON);
                            Main.townCoreLocation.put(defender, next);
                            
                            // Visual Effects
                            world.strikeLightningEffect(next);
                            world.spawnParticle(org.bukkit.Particle.PORTAL, next.clone().add(0.5, 0.5, 0.5), 100, 0.5, 0.5, 0.5, 0.1);
                            world.spawnParticle(org.bukkit.Particle.DRAGON_BREATH, next.clone().add(0.5, 0.5, 0.5), 50, 0.3, 0.3, 0.3, 0.05);

                            Bukkit.broadcastMessage("§4§l[WAR] §r§cThe §f" + defender + " §cTown Core has teleported!");
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void restoreCore(War war) {
        String defender = war.defenderTown;
        Location current = Main.townCoreLocation.get(defender);
        Location original = originalCoreLocations.remove(defender);
        
        Integer taskId = teleportTasks.remove(defender);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        if (original != null) {
            if (current != null) current.getBlock().setType(Material.AIR);
            original.getBlock().setType(Material.BEACON);
            Main.townCoreLocation.put(defender, original);
        }
    }
}