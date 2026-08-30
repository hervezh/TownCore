package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.silvarys.TownInfoGUI;
import com.silvarys.TownTopGUI;
import com.silvarys.TownListGUI;
import com.silvarys.TownGUI;
import com.silvarys.TownIncomeGUI;
import com.silvarys.TownUpgradesGUI;
import com.silvarys.Main;
import com.silvarys.InviteManager;
import com.silvarys.StorageManager;
import com.silvarys.WarManager;
import com.silvarys.TownIncomeManager;
import com.silvarys.TutorialManager;
import com.silvarys.TownLevelManager;
import com.silvarys.WarBossBarManager;
import com.silvarys.RadarManager;
import com.silvarys.ChatListener;
import com.silvarys.TownUpgradesManager;
import com.silvarys.LockManager;

import java.util.*;

public class TownCommand implements CommandExecutor {

    private static final long THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000;
    private static final long TOWN_COMMAND_COOLDOWN_MS = 2000L;
    private static final long CONFIRMATION_TIMEOUT_MS = 60L * 1000L;
    private static final double MAX_WITHDRAW_AMOUNT = 1000.0;
    private static final int MAX_ALLIES = 10;

    private final Map<UUID, Long> townCommandCooldowns = new HashMap<>();

    private final Map<UUID, Long> leaveConfirmations = new HashMap<>();
    private final Map<UUID, Long> disbandConfirmations = new HashMap<>();
    private final Map<UUID, PendingConfirmation> kickConfirmations = new HashMap<>();
    private final Map<UUID, PendingConfirmation> warConfirmations = new HashMap<>();
    private final Map<UUID, PendingConfirmation> restoreConfirmations = new HashMap<>();
    private final Map<UUID, Long> revoltConfirmations = new HashMap<>();
    private final Map<UUID, Long> spawnCooldowns = new HashMap<>();
    private final Map<UUID, Integer> activeSpawns = new HashMap<>();

    private static class PendingConfirmation {
        private final String value;
        private final long createdAt;

        private PendingConfirmation(String value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            TownGUI.open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (!isConfirmationSubcommand(subCommand) && isOnTownCommandCooldown(player)) {
            return true;
        }

        switch (subCommand) {
            case "info" -> handleInfo(player, args);
            case "here" -> handleHere(player);
            case "core" -> handleCore(player);
            case "spawn" -> handleSpawn(player);
            case "setspawn" -> handleSetSpawn(player);
            case "members" -> handleMembers(player, args);
            case "online" -> handleOnline(player);
            case "bank" -> handleBank(player);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "income" -> handleIncome(player);
            case "claim" -> handleClaim(player);
            case "unclaim" -> handleUnclaim(player);
            case "claims" -> handleClaims(player);
            case "relations" -> handleRelations(player);
            case "level" -> handleLevel(player);
            case "map" -> handleMap(player);
            case "border" -> handleBorder(player);
            case "top" -> handleTop(player);
            case "inspect" -> handleInspect(player, args);
            case "leave" -> handleLeaveConfirm(player);
            case "confirmleave" -> handleLeave(player);
            case "disband" -> handleDisbandConfirm(player);
            case "confirmdisband" -> handleDisband(player);
            case "invite" -> handleInvite(player, args);
            case "invites" -> InviteManager.showInvites(player);
            case "accept" -> InviteManager.acceptInvite(player);
            case "deny" -> InviteManager.denyInvite(player);
            case "kick" -> handleKickConfirm(player, args);
            case "confirmkick" -> handleKick(player, args);
            case "promote" -> handlePromote(player, args);
            case "demote" -> handleDemote(player, args);
            case "ally" -> handleAlly(player, args);
            case "unally" -> handleUnally(player, args);
            case "enemy" -> handleEnemy(player, args);
            case "unenemy" -> handleUnenemy(player, args);
            case "declarewar" -> handleDeclareWarConfirm(player, args);
            case "confirmdeclarewar" -> handleDeclareWar(player, args);
            case "revolt" -> handleRevoltConfirm(player);
            case "confirmrevolt" -> handleRevolt(player);
            case "warinfo" -> handleWarInfo(player);
            case "surrender" -> handleSurrender(player);
            case "list" -> handleList(player);
            case "allylist" -> handleAllyList(player, args);
            case "enemylist" -> handleEnemyList(player, args);
            case "hostilelist" -> handleHostileList(player, args);
            case "backup" -> handleBackup(player, args);
            case "backups" -> handleBackups(player, args);
            case "restore" -> handleRestore(player, args);
            case "confirmrestore" -> handleConfirmRestore(player, args);
            case "save" -> handleSave(player);
            case "reload" -> handleReload(player);
            case "rename" -> handleRename(player, args);
            case "renamerequest" -> handleRenameRequest(player);
            case "approvename" -> handleApproveName(player, args);
            case "denyname" -> handleDenyName(player, args);
            case "chat" -> handleChat(player);
            case "allychat" -> handleAllyChat(player);
            case "motd" -> handleMotd(player, args);
            case "clearmotd" -> handleClearMotd(player);
            case "mobs" -> handleMobs(player, args);
            case "title" -> handleTitle(player, args);
            case "logs" -> handleLogs(player, args);
            case "calltoarms" -> handleCallToArms(player, args);
            case "upgrades" -> handleUpgrades(player);
            case "plot" -> handlePlot(player, args);
            case "tutorial" -> handleTutorial(player, args);
            case "setcolor" -> handleSetColor(player, args);

            case "adminclaim" -> handleAdminClaim(player, args);
            case "admindisband" -> handleAdminDisband(player, args);
            case "adminsetlevel" -> handleAdminSetLevel(player, args);
            case "adminbank" -> handleAdminBank(player, args);
            case "infochat" -> handleInfoChat(player, args);
            case "rollback" -> handleRollback(player, args);
            case "rollbackinfo" -> handleRollbackInfo(player, args);
            case "adminwars" -> handleAdminWars(player);
            case "adminstartwar" -> handleAdminStartWar(player, args);
            case "adminendwar" -> handleAdminEndWar(player, args);
            case "warterminate" -> handleWarTerminate(player, args);
            case "adminwarauto" -> handleAdminWarAuto(player, args);

            case "help" -> {
                if (args.length > 1) {
                    int page = 1;
                    if (args.length > 2) {
                        try {
                            page = Integer.parseInt(args[2]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    sendCategoryHelp(player, args[1], page);
                } else {
                    sendHelp(player);
                }
            }
            default -> player.sendMessage("§cUnknown subcommand. Type /town help for help.");
        }

        return true;
    }

    private boolean isConfirmationSubcommand(String subCommand) {
        return subCommand.equals("confirmleave")
                || subCommand.equals("confirmkick")
                || subCommand.equals("confirmdeclarewar")
                || subCommand.equals("confirmrestore")
                || subCommand.equals("confirmdisband");
    }

    private boolean isOnTownCommandCooldown(Player player) {
        if (player.hasPermission("silvarys.town.cooldown.bypass") || player.hasPermission("silvarys.staff")) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUsed = townCommandCooldowns.getOrDefault(uuid, 0L);
        long timePassed = now - lastUsed;

        if (timePassed < TOWN_COMMAND_COOLDOWN_MS) {
            long timeLeft = TOWN_COMMAND_COOLDOWN_MS - timePassed;
            double secondsLeft = timeLeft / 1000.0;

            player.sendMessage("§cPlease slow down! Try again in §e"
                    + String.format("%.1f", secondsLeft)
                    + "s§c.");
            return true;
        }

        townCommandCooldowns.put(uuid, now);
        return false;
    }

    private void saveTownData() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        StorageManager.saveData(plugin);
    }

    private void addSimpleConfirmation(Map<UUID, Long> confirmations, Player player) {
        confirmations.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean hasValidSimpleConfirmation(Map<UUID, Long> confirmations, Player player, String commandName) {
        UUID uuid = player.getUniqueId();
        Long createdAt = confirmations.get(uuid);

        if (createdAt == null) {
            player.sendMessage("§cYou need to run §f/town " + commandName + " §cfirst.");
            return false;
        }

        if (System.currentTimeMillis() - createdAt > CONFIRMATION_TIMEOUT_MS) {
            confirmations.remove(uuid);
            player.sendMessage("§cThat confirmation expired. Run §f/town " + commandName + " §cagain.");
            return false;
        }

        confirmations.remove(uuid);
        return true;
    }

    private void addValueConfirmation(Map<UUID, PendingConfirmation> confirmations, Player player, String value) {
        confirmations.put(player.getUniqueId(), new PendingConfirmation(value));
    }

    private boolean hasValidValueConfirmation(Map<UUID, PendingConfirmation> confirmations, Player player, String expectedValue, String commandName) {
        UUID uuid = player.getUniqueId();
        PendingConfirmation confirmation = confirmations.get(uuid);

        if (confirmation == null) {
            player.sendMessage("§cYou need to run §f/town " + commandName + " §cfirst.");
            return false;
        }

        if (System.currentTimeMillis() - confirmation.createdAt > CONFIRMATION_TIMEOUT_MS) {
            confirmations.remove(uuid);
            player.sendMessage("§cThat confirmation expired. Run §f/town " + commandName + " §cagain.");
            return false;
        }

        if (expectedValue != null && !confirmation.value.equalsIgnoreCase(expectedValue)) {
            player.sendMessage("§cThat confirmation does not match your last request.");
            player.sendMessage("§7Run §f/town " + commandName + " §7again.");
            return false;
        }

        confirmations.remove(uuid);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(" ");
        player.sendMessage(Component.text("  ✦ Town Commands ✦")
                .color(TextColor.color(0xFFD166))
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Pick a category to get started.")
                .color(TextColor.color(0x888888)));
        player.sendMessage(" ");

        player.sendMessage(Component.text("  ")
                .append(Component.text("• ")
                        .color(TextColor.color(0x666666)))
                .append(Component.text("Town Commands")
                        .color(TextColor.color(0xf4a261))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help town"))
                        .hoverEvent(HoverEvent.showText(Component.text("§eClick §7to view town commands\n§7Claim, bank, spawn, and more")))));

        player.sendMessage(Component.text("  ")
                .append(Component.text("• ")
                        .color(TextColor.color(0x666666)))
                .append(Component.text("Diplomacy")
                        .color(TextColor.color(0xe63946))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help diplomacy"))
                        .hoverEvent(HoverEvent.showText(Component.text("§eClick §7to view diplomacy commands\n§7Allies, enemies, wars")))));

        player.sendMessage(Component.text("  ")
                .append(Component.text("• ")
                        .color(TextColor.color(0x666666)))
                .append(Component.text("Level & Stats")
                        .color(TextColor.color(0x52b788))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help stats"))
                        .hoverEvent(HoverEvent.showText(Component.text("§eClick §7to view stat commands\n§7Level, XP, leaderboard")))));

        player.sendMessage(Component.text("  ")
                .append(Component.text("• ")
                        .color(TextColor.color(0x666666)))
                .append(Component.text("Members")
                        .color(TextColor.color(0x457b9d))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help members"))
                        .hoverEvent(HoverEvent.showText(Component.text("§eClick §7to view member commands\n§7Invite, kick, promote")))));

        player.sendMessage(Component.text("  ")
                .append(Component.text("• ")
                        .color(TextColor.color(0x666666)))
                .append(Component.text("Chat")
                        .color(TextColor.color(0x9b5de5))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help chat"))
                        .hoverEvent(HoverEvent.showText(Component.text("§eClick §7to view chat commands\n§7Town chat, ally chat")))));

        if (player.hasPermission("silvarys.staff")) {
            player.sendMessage(Component.text("  ")
                    .append(Component.text("• ")
                            .color(TextColor.color(0x666666)))
                    .append(Component.text("Staff & Admin")
                            .color(TextColor.color(0xd000ff))
                            .decoration(TextDecoration.BOLD, true)
                            .clickEvent(ClickEvent.runCommand("/town help staff"))
                            .hoverEvent(HoverEvent.showText(Component.text("§eClick §7to view staff commands\n§7Backups, admin tools, war management")))));
        }

        player.sendMessage(" ");
    }

    private static final String[][] HELP_ENTRIES_TOWN = {
            {"/town", "Opens the town GUI"},
            {"/town help", "Shows command help"},
            {"/town info", "Shows your town info"},
            {"/town here", "Shows info about your current chunk"},
            {"/town core", "Shows your Town Core information"},
            {"/town spawn", "Teleport to your town spawn"},
            {"/town setspawn", "Set your town spawn (3 day cooldown)"},
            {"/town claim", "Claim the chunk you're standing in"},
            {"/town unclaim", "Unclaim the chunk you're standing in"},
            {"/town claims", "Shows your town claim usage"},
            {"/town relations", "Shows allies, enemies, hostile towns, and wars"},
            {"/town map", "Shows a chunk map around you"},
            {"/town border", "Shows particles around your town borders"},
            {"/town bank", "Shows town bank, upkeep, and survival time"},
            {"/town deposit <amount>", "Deposit to town bank"},
            {"/town withdraw <amount>", "Withdraw from town bank (max $1000)"},
            {"/town income", "View and collect town income resources"},
            {"/town list", "List all towns"},
            {"/town top", "Shows the top towns leaderboard"},
            {"/town leave", "Leave your town"},
            {"/town disband", "Permanently delete your town"},
            {"/town rename <name>", "Rename your town (one time only)"},
            {"/town motd <message>", "Set town MOTD"},
            {"/town clearmotd", "Clear town MOTD"},
            {"/town members", "Shows town members"},
            {"/town online", "Shows online town members"},
            {"/town title <player> <title>", "Set a town member's title"},
            {"/town mobs on/off/status", "Toggle hostile mobs at level 5+"},
            {"/town logs", "View recent town audit logs"}
    };

    private static final String[][] HELP_ENTRIES_DIPLOMACY = {
            {"/town ally <town>", "Send an ally request to a town"},
            {"/town ally accept <town>", "Accept an ally request"},
            {"/town ally deny <town>", "Deny an ally request"},
            {"/town unally <town>", "Remove an ally"},
            {"/town enemy <town>", "Mark a town as enemy"},
            {"/town unenemy <town>", "Remove an enemy"},
            {"/town relations", "Shows allies, enemies, hostile towns, and wars"},
            {"/town allylist", "View a detailed list of your allies"},
            {"/town enemylist", "View a detailed list of your enemies"},
            {"/town hostilelist", "View towns that marked you as enemy"},
            {"/town declarewar <town>", "Declare war on an enemy town"},
            {"/town warinfo", "Shows war info"},
            {"/town surrender", "Surrender the war"}
    };

    private static final String[][] HELP_ENTRIES_STATS = {
            {"/town level", "Shows town level and XP progress"},
            {"/town top", "Shows the top towns leaderboard"},
            {"/town claims", "Shows your town claim usage"},
            {"/town border", "Shows particles around your town borders"},
            {"/town relations", "Shows your town relationships"},
            {"/town info", "Shows town info with allies, enemies, and hostile towns"},
            {"/town core", "Shows your Town Core information"},
            {"/town bank", "Shows town bank and upkeep status"},
            {"/town logs", "Shows recent town audit logs"}
    };

    private static final String[][] HELP_ENTRIES_MEMBERS = {
            {"/town members", "Shows town members"},
            {"/town online", "Shows online town members"},
            {"/town invite <player>", "Invite a player"},
            {"/town invites", "Shows your pending town invite"},
            {"/town accept", "Accept a town invite"},
            {"/town deny", "Deny a town invite"},
            {"/town kick <player>", "Kick a member"},
            {"/town promote <player>", "Promote member to assistant"},
            {"/town demote <player>", "Demote assistant to member"},
            {"/town title <player> <title>", "Set a town member's title"}
    };

    private static final String[][] HELP_ENTRIES_CHAT = {
            {"/town chat", "Toggle town chat on/off"},
            {"/town allychat", "Toggle ally chat on/off"}
    };

    private static final String[][] HELP_ENTRIES_STAFF = {
            {"/town inspect <town>", "Inspect full town information"},
            {"/town logs <town>", "View a town's audit logs"},
            {"/town backup", "Create a town data backup"},
            {"/town backups", "List town backups"},
            {"/town restore <backup>", "Opens restore confirmation"},
            {"/town confirmrestore <backup>", "Confirm restoring a backup"},
            {"/town save", "Manually save town data"},
            {"/town reload", "Reload town data from towns.yml"},
            {"/town renamerequest", "View pending town rename requests"},
            {"/town approvename <town>", "Approve a rename request"},
            {"/town denyname <town>", "Deny a rename request"},
            {"/town rollback <town> [min]", "Roll back grief damage in a town"},
            {"/town rollbackinfo <town>", "View rollback data count for a town"},
            {"/town adminclaim <town>", "Force claim a chunk for a town"},
            {"/town admindisband <town>", "Force disband a town"},
            {"/town adminsetlevel <town> <lv>", "Set a town level"},
            {"/town adminbank <town> <amt>", "Set a town bank balance"},
            {"/town adminwars", "List all wars and war IDs"},
            {"/town adminstartwar <town> <min>", "Start a war session"},
            {"/town adminendwar <warId>", "End an active war session by ID"},
            {"/town warterminate <warId>", "Terminate/delete a war"},
            {"/town adminwarauto on/off/status", "Toggle automatic war sessions"}
    };

    private void sendCategoryHelp(Player player, String category, int page) {
        String cat = category.toLowerCase();

        String title;
        int titleColor;
        String[][] entries;

        switch (cat) {
            case "town" -> {
                title = "Town Commands";
                titleColor = 0xf4a261;
                entries = HELP_ENTRIES_TOWN;
            }
            case "diplomacy" -> {
                title = "Diplomacy Commands";
                titleColor = 0xe63946;
                entries = HELP_ENTRIES_DIPLOMACY;
            }
            case "stats" -> {
                title = "Level & Stats";
                titleColor = 0x52b788;
                entries = HELP_ENTRIES_STATS;
            }
            case "members" -> {
                title = "Member Commands";
                titleColor = 0x457b9d;
                entries = HELP_ENTRIES_MEMBERS;
            }
            case "chat" -> {
                title = "Chat Commands";
                titleColor = 0x9b5de5;
                entries = HELP_ENTRIES_CHAT;
            }
            case "staff" -> {
                if (!player.hasPermission("silvarys.staff")) {
                    player.sendMessage("§cYou don't have permission to view staff commands!");
                    return;
                }
                title = "Staff & Admin Commands";
                titleColor = 0xd000ff;
                entries = HELP_ENTRIES_STAFF;
            }
            default -> {
                player.sendMessage("§cUnknown help category.");
                player.sendMessage("§7Categories: §etown§7, §ediplomacy§7, §estats§7, §emembers§7, §echat");
                if (player.hasPermission("silvarys.staff")) {
                    player.sendMessage("§7Staff category: §estaff");
                }
                return;
            }
        }

        int perPage = 5;
        int totalPages = (int) Math.ceil((double) entries.length / perPage);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, entries.length);

        // Header
        player.sendMessage(" ");
        player.sendMessage(Component.text("  ✦ " + title + " ")
                .color(TextColor.color(titleColor))
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("(" + page + "/" + totalPages + ")")
                        .color(TextColor.color(0x888888))
                        .decoration(TextDecoration.BOLD, false)));
        player.sendMessage(" ");

        // Command entries
        for (int i = startIndex; i < endIndex; i++) {
            String cmd = entries[i][0];
            String desc = entries[i][1];

            // Extract the base command (first word after /town) for click
            String baseCmd = cmd.split(" <")[0].split(" \\[")[0]; // Remove <args> and [args]

            player.sendMessage(Component.text("  ")
                    .append(Component.text("Ã¢â‚¬Â¢ ")
                            .color(TextColor.color(titleColor)))
                    .append(Component.text(cmd)
                            .color(TextColor.color(0xFFFF55))
                            .clickEvent(ClickEvent.suggestCommand(baseCmd))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text(cmd + "\n")
                                            .color(TextColor.color(0xFFFF55))
                                            .append(Component.text(desc)
                                                    .color(TextColor.color(0xAAAAAA)))
                                            .append(Component.text("\n\n"))
                                            .append(Component.text("Click to type this command")
                                                    .color(TextColor.color(0x55FF55)))
                            )))
                    .append(Component.text(" ")
                            .color(TextColor.color(0x555555)))
                    .append(Component.text(desc)
                            .color(TextColor.color(0x999999))));
        }

        // Footer with navigation
        player.sendMessage(" ");

        if (totalPages > 1) {
            Component nav = Component.text("  ");

            if (page > 1) {
                nav = nav.append(Component.text("« Prev")
                        .color(TextColor.color(titleColor))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help " + cat + " " + (page - 1)))
                        .hoverEvent(HoverEvent.showText(Component.text("§7Page " + (page - 1)))));
            } else {
                nav = nav.append(Component.text("« Prev")
                        .color(TextColor.color(0x555555)));
            }

            nav = nav.append(Component.text("  §8→  "));

            // Page dots
            for (int i = 1; i <= totalPages; i++) {
                if (i == page) {
                    nav = nav.append(Component.text("●")
                            .color(TextColor.color(titleColor)));
                } else {
                    nav = nav.append(Component.text("○")
                            .color(TextColor.color(0x666666))
                            .clickEvent(ClickEvent.runCommand("/town help " + cat + " " + i))
                            .hoverEvent(HoverEvent.showText(Component.text("§7Page " + i))));
                }
                if (i < totalPages) {
                    nav = nav.append(Component.text(" "));
                }
            }

            nav = nav.append(Component.text("  §8→  "));

            if (page < totalPages) {
                nav = nav.append(Component.text("Next »")
                        .color(TextColor.color(titleColor))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town help " + cat + " " + (page + 1)))
                        .hoverEvent(HoverEvent.showText(Component.text("§7Page " + (page + 1)))));
            } else {
                nav = nav.append(Component.text("Next »")
                        .color(TextColor.color(0x555555)));
            }

            player.sendMessage(nav);
        }

        // Back button
        player.sendMessage(Component.text("  ")
                .append(Component.text("[© Back to Help]")
                        .color(TextColor.color(0x888888))
                        .clickEvent(ClickEvent.runCommand("/town help"))
                        .hoverEvent(HoverEvent.showText(Component.text("§7Return to category list")))));
        player.sendMessage(" ");
    }

    private void handleSetColor(Player player, String[] args) {
        String townName = getTown(player);
        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }
        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can change the town color!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town setcolor <&code or &#hex>");
            return;
        }
        String color = args[1];
        Main.townTagColor.put(townName, color);
        player.sendMessage("§aTown color updated to: " + ChatListener.translateColors(color) + townName);
        saveTownData();
    }

    private String getTown(Player player) {
        return Main.playerTown.get(player.getUniqueId());
    }

    private boolean isRulerOrAssistant(Player player) {
        String role = Main.playerRole.get(player.getUniqueId());
        return role != null && (role.equals("ruler") || role.equals("assistant"));
    }

    private boolean isRuler(Player player) {
        String role = Main.playerRole.get(player.getUniqueId());
        return role != null && role.equals("ruler");
    }

    private String getTownNameFromArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) builder.append(" ");
            builder.append(args[i]);
        }

        return builder.toString();
    }

    private String findTownName(String input) {
        if (Main.townOwner.containsKey(input)) return input;
        for (String name : Main.townOwner.keySet()) {
            if (name.equalsIgnoreCase(input)) return name;
        }
        return null;
    }

    private void handleTutorial(Player player, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        TutorialManager.sendTutorial(player, page);
    }

    private void handleInfo(Player player, String[] args) {
        String townName;
 
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            townName = findTownName(input);
            if (townName == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        } else {
            townName = getTown(player);
            if (townName == null) {
                player.sendMessage("§cUsage: /town info <townname>");
                return;
            }
        }
 
        TownInfoGUI.open(player, townName);
    }
 
    private void handleInfoChat(Player player, String[] args) {
        // This is primarily for the GUI to trigger a chat output
        if (args.length < 2) return;
        String townName = getTownNameFromArgs(args, 1);
        sendTownInfoToChat(player, townName);
    }

    private void handleHere(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Main.getChunkKey(chunk);

        String ownerTown = getTownAtChunk(chunkKey);
        String playerTown = getTown(player);

        player.sendMessage("§6§l--- Chunk Info ---");
        player.sendMessage("§eWorld: §f" + chunk.getWorld().getName());
        player.sendMessage("§eChunk: §f" + chunk.getX() + ", " + chunk.getZ());

        if (ownerTown == null) {
            player.sendMessage("§eOwner: §7Wilderness");
            player.sendMessage("§eRelation: §7None");
            return;
        }

        player.sendMessage("§eOwner: §f" + ownerTown);

        if (playerTown == null) {
            player.sendMessage("§eRelation: §eNeutral");
            return;
        }

        player.sendMessage("§eRelation: " + getRelationDisplay(playerTown, ownerTown));
    }

    private void handleCore(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        Location core = Main.townCoreLocation.get(townName);

        player.sendMessage("§6§l--- Town Core ---");

        if (core == null || core.getWorld() == null) {
            player.sendMessage("§cYour Town Core location is missing!");
            return;
        }

        player.sendMessage("§eWorld: §f" + core.getWorld().getName());
        player.sendMessage("§eLocation: §fX:" + core.getBlockX()
                + " Y:" + core.getBlockY()
                + " Z:" + core.getBlockZ());
        player.sendMessage("§eChunk: §f" + core.getChunk().getX() + ", " + core.getChunk().getZ());

        WarManager.War war = WarManager.getWarByDefenderTown(townName);

        if (war != null && war.activeSession) {
            player.sendMessage("§eStatus: §cUnder Attack");
            player.sendMessage("§eWar Health: §f" + WarManager.getCoreHealthPercent(townName) + "%");
        } else {
            player.sendMessage("§eStatus: §aProtected");
            player.sendMessage("§eWar Health: §f100%");
        }
    }

    private void handleOnline(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        UUID ownerUUID = Main.townOwner.get(townName);
        UUID assistantUUID = Main.townAssistant.get(townName);

        player.sendMessage("§6§l--- Online Members of " + townName + " ---");

        Player owner = ownerUUID != null ? Bukkit.getPlayer(ownerUUID) : null;
        player.sendMessage("§c§lRuler:");

        if (owner != null && owner.isOnline()) {
            player.sendMessage(buildMemberHoverLine(ownerUUID, "  " + owner.getName()));
        } else {
            player.sendMessage("§7  Offline");
        }

        player.sendMessage("§6§lAssistant:");
        Player assistant = assistantUUID != null ? Bukkit.getPlayer(assistantUUID) : null;

        if (assistant != null && assistant.isOnline()) {
            player.sendMessage(buildMemberHoverLine(assistantUUID, "  " + assistant.getName()));
        } else {
            player.sendMessage("§7  Offline/None");
        }

        player.sendMessage("§e§lMembers:");

        int onlineMembers = 0;

        for (UUID uuid : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            if (uuid.equals(ownerUUID) || uuid.equals(assistantUUID)) continue;

            Player member = Bukkit.getPlayer(uuid);

            if (member != null && member.isOnline()) {
                player.sendMessage(buildMemberHoverLine(uuid, "  " + member.getName()));
                onlineMembers++;
            }
        }

        if (onlineMembers == 0) {
            player.sendMessage("§7  No regular members online.");
        }
    }

    private void handleBank(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        double balance = Main.townBank.getOrDefault(townName, 0.0);
        double upkeep = Main.getDailyUpkeepCost();
        int survivalDays = Main.getTownSurvivalDays(townName);

        player.sendMessage("§6§l--- Town Bank: " + townName + " ---");
        player.sendMessage("§eBalance: §f$" + String.format("%.2f", balance));
        player.sendMessage("§eDaily Upkeep: §f$" + String.format("%.2f", upkeep));

        if (survivalDays <= 0) {
            player.sendMessage("§eSurvival Time: §cLess than 1 day");
            player.sendMessage("§cYour town needs more money to survive upkeep!");
        } else if (survivalDays <= 2) {
            player.sendMessage("§eSurvival Time: §c" + survivalDays + " day" + (survivalDays == 1 ? "" : "s"));
            player.sendMessage("§cWarning: Your town bank is running low!");
        } else if (survivalDays <= 7) {
            player.sendMessage("§eSurvival Time: §e" + survivalDays + " days");
            player.sendMessage("§7Your town is okay, but should deposit more soon.");
        } else {
            player.sendMessage("§eSurvival Time: §a" + survivalDays + " days");
            player.sendMessage("§aYour town bank is healthy.");
        }

        player.sendMessage(" ");
        player.sendMessage("§7Use §f/town deposit <amount> §7to add money.");

        if (isRulerOrAssistant(player)) {
            player.sendMessage("§7Use §f/town withdraw <amount> §7to withdraw money.");
        }
    }

    private void handleIncome(Player player) {
        TownIncomeGUI.open(player);
    }

    private void handleClaims(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        int level = Main.townLevel.getOrDefault(townName, 1);
        int currentClaims = Main.townChunks.getOrDefault(townName, Collections.emptySet()).size();
        int maxClaims = Main.getMaxChunks(level);
        int remaining = Math.max(0, maxClaims - currentClaims);

        player.sendMessage("§6§l--- Town Claims ---");
        player.sendMessage("§eTown: §f" + townName);
        player.sendMessage("§eLevel: §f" + level);
        player.sendMessage("§eClaims Used: §f" + currentClaims + "/" + maxClaims);
        player.sendMessage("§eRemaining Claims: §f" + remaining);
    }

    private void handleRelations(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        Set<String> allies = Main.townAllies.getOrDefault(townName, Collections.emptySet());
        Set<String> enemies = Main.townEnemies.getOrDefault(townName, Collections.emptySet());
        Set<String> hostile = getTownsThatEnemy(townName);
        Set<String> wars = Main.townWars.getOrDefault(townName, Collections.emptySet());

        player.sendMessage("§6§l--- Town Relations ---");
        player.sendMessage("§eTown: §f" + townName);
        player.sendMessage("§aAllies: §f" + formatTownSet(allies));
        player.sendMessage("§cEnemies: §f" + formatTownSet(enemies));
        player.sendMessage("§6Hostile: §f" + formatTownSet(hostile));
        player.sendMessage("§4Wars: §f" + formatTownSet(wars));
    }

    private void handleSpawn(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        Location spawn = Main.townSpawn.get(townName);

        if (spawn == null || spawn.getWorld() == null) {
            player.sendMessage("§cYour town spawn is not set!");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (spawnCooldowns.containsKey(uuid)) {
            long timeLeft = (spawnCooldowns.get(uuid) + 30000) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage("§cYou must wait " + (timeLeft / 1000) + " seconds before using /town spawn again.");
                return;
            }
        }

        if (activeSpawns.containsKey(uuid)) {
            player.sendMessage("§cYou are already teleporting!");
            return;
        }

        player.sendMessage("§aTeleporting to town spawn in 5 seconds... Do not move or take damage!");

        Location initialLoc = player.getLocation().clone();
        double[] initialHealth = {player.getHealth()};

        int taskId = new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeSpawns.remove(uuid);
                    this.cancel();
                    return;
                }

                if (player.getLocation().getWorld() != initialLoc.getWorld() || player.getLocation().distanceSquared(initialLoc) > 0.25) {
                    player.sendMessage("§cTeleport cancelled because you moved!");
                    activeSpawns.remove(uuid);
                    this.cancel();
                    return;
                }

                if (player.getHealth() < initialHealth[0]) {
                    player.sendMessage("§cTeleport cancelled because you took damage!");
                    activeSpawns.remove(uuid);
                    this.cancel();
                    return;
                }
                
                initialHealth[0] = player.getHealth();

                ticks += 5;
                if (ticks >= 100) { 
                    player.teleport(spawn);
                    player.sendMessage("§aTeleported to your town spawn!");
                    spawnCooldowns.put(uuid, System.currentTimeMillis());
                    activeSpawns.remove(uuid);
                    this.cancel();
                }
            }
        }.runTaskTimer(JavaPlugin.getProvidingPlugin(TownCommand.class), 5L, 5L).getTaskId();

        activeSpawns.put(uuid, taskId);
    }

    private void handleSetSpawn(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can set town spawn!");
            return;
        }

        long now = System.currentTimeMillis();
        Long lastSet = Main.townSpawnCooldown.get(townName);

        if (lastSet != null && now - lastSet < THREE_DAYS_MS) {
            long remaining = THREE_DAYS_MS - (now - lastSet);
            long hours = remaining / 3600000;
            long minutes = (remaining % 3600000) / 60000;

            player.sendMessage("§cTown spawn is on cooldown! §f" + hours + "h " + minutes + "m §cremaining.");
            return;
        }

        Main.townSpawn.put(townName, player.getLocation());
        Main.townSpawnCooldown.put(townName, now);

        Main.logTownAction(townName, player.getName() + " set the town spawn.");
        saveTownData();

        player.sendMessage("§aTown spawn set! §7(Next change available in 3 days)");
    }

    private void handleMembers(Player player, String[] args) {
        String townName;
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            townName = findTownName(input);
            if (townName == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        } else {
            townName = getTown(player);
            if (townName == null) {
                player.sendMessage("§cYou don't have a town!");
                return;
            }
        }

        UUID ownerUUID = Main.townOwner.get(townName);
        UUID assistantUUID = Main.townAssistant.get(townName);

        player.sendMessage("§6§l--- Members of " + townName + " ---");
        player.sendMessage("§7Hover over a member to see title, role, and last online.");

        if (ownerUUID != null) {
            player.sendMessage("§c§lRuler:");
            player.sendMessage(buildMemberHoverLine(ownerUUID, "  " + getOfflineName(ownerUUID)));
        }

        player.sendMessage("§6§lAssistant:");

        if (assistantUUID != null) {
            player.sendMessage(buildMemberHoverLine(assistantUUID, "  " + getOfflineName(assistantUUID)));
        } else {
            player.sendMessage("§7  None");
        }

        player.sendMessage("§e§lMembers:");

        int normalMembers = 0;

        for (UUID uuid : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            if (uuid.equals(ownerUUID) || uuid.equals(assistantUUID)) continue;

            player.sendMessage(buildMemberHoverLine(uuid, "  " + getOfflineName(uuid)));
            normalMembers++;
        }

        if (normalMembers == 0) {
            player.sendMessage("§7  None");
        }
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town deposit <amount>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);

            if (amount <= 0) {
                player.sendMessage("§cAmount must be greater than 0!");
                return;
            }

            if (Main.economy != null) {
                if (!Main.economy.has(player, amount)) {
                    player.sendMessage("§cYou don't have enough money! You have §f$"
                            + String.format("%.2f", Main.economy.getBalance(player)));
                    return;
                }

                Main.economy.withdrawPlayer(player, amount);
            }

            Main.townBank.put(townName, Main.townBank.getOrDefault(townName, 0.0) + amount);

            UpkeepManager.checkTownFundsWarning(townName);

            Main.logTownAction(townName, player.getName() + " deposited $" + String.format("%.2f", amount) + " into the town bank.");
            saveTownData();

            player.sendMessage("§aDeposited §f$" + String.format("%.2f", amount)
                    + " §ato " + townName + "'s bank!");

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town withdraw <amount>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can withdraw from the town bank!");
            return;
        }

        if (Main.economy == null) {
            player.sendMessage("§cEconomy is not enabled, so withdrawals are disabled.");
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);

            if (amount <= 0) {
                player.sendMessage("§cAmount must be greater than 0!");
                return;
            }

            if (amount > MAX_WITHDRAW_AMOUNT) {
                player.sendMessage("§cYou can only withdraw up to §f$" + String.format("%.2f", MAX_WITHDRAW_AMOUNT) + " §cat once.");
                return;
            }

            double balance = Main.townBank.getOrDefault(townName, 0.0);

            if (balance < amount) {
                player.sendMessage("§cYour town bank does not have enough money!");
                player.sendMessage("§7Town bank: §f$" + String.format("%.2f", balance));
                return;
            }

            Main.townBank.put(townName, balance - amount);
            Main.economy.depositPlayer(player, amount);

            UpkeepManager.checkTownFundsWarning(townName);

            Main.logTownAction(townName, player.getName() + " withdrew $" + String.format("%.2f", amount) + " from the town bank.");
            saveTownData();

            player.sendMessage("§aWithdrew §f$" + String.format("%.2f", amount) + " §afrom " + townName + "'s bank!");

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
        }
    }

    private void handleClaim(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can claim chunks!");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Main.getChunkKey(chunk);

        for (Map.Entry<String, Set<String>> entry : Main.townChunks.entrySet()) {
            if (entry.getValue().contains(chunkKey)) {
                if (entry.getKey().equals(townName)) {
                    player.sendMessage("§cThis chunk is already claimed by your town!");
                } else {
                    player.sendMessage("§cThis chunk is already claimed by §f" + entry.getKey() + "§c!");
                }

                return;
            }
        }

        int level = Main.townLevel.getOrDefault(townName, 1);
        int maxChunks = Main.getMaxChunks(level);
        int currentChunks = Main.townChunks.getOrDefault(townName, new HashSet<>()).size();

        if (currentChunks >= maxChunks) {
            player.sendMessage("§cChunk limit reached! §f(" + currentChunks + "/" + maxChunks + ")");
            player.sendMessage("§cLevel up your town to claim more chunks!");
            return;
        }

        if (!isClaimAdjacentToTown(townName, chunk)) {
            player.sendMessage("§cYou can only claim chunks connected to your town!");
            player.sendMessage("§7New claims must touch one of your existing claims on the north, south, east, or west side.");
            return;
        }

        Main.townChunks.putIfAbsent(townName, new HashSet<>());
        Main.townChunks.get(townName).add(chunkKey);

        Main.logTownAction(townName, player.getName() + " claimed chunk " + chunkKey + ".");
        saveTownData();

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        TownVisualManager.showClaimBorder(player, chunk, plugin);

        player.sendMessage("§a§lChunk Claimed!");
        player.sendMessage("§7Claimed by: §f" + townName);
        player.sendMessage("§7Claims: §f" + (currentChunks + 1) + "/" + maxChunks);
        player.sendMessage("§7A temporary border preview has been shown.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    private void handleUnclaim(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can unclaim chunks!");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Main.getChunkKey(chunk);

        Set<String> claims = Main.townChunks.get(townName);

        if (claims == null || !claims.contains(chunkKey)) {
            String ownerTown = getTownAtChunk(chunkKey);

            if (ownerTown != null) {
                player.sendMessage("§cThis chunk is claimed by §f" + ownerTown + "§c, not your town!");
            } else {
                player.sendMessage("§cThis chunk is not claimed by your town!");
            }

            return;
        }

        if (claims.size() <= 1) {
            player.sendMessage("§cYou cannot unclaim your town's last chunk!");
            return;
        }

        Location coreLocation = Main.townCoreLocation.get(townName);

        if (coreLocation != null && coreLocation.getWorld() != null) {
            String coreChunkKey = Main.getChunkKey(coreLocation.getChunk());

            if (chunkKey.equals(coreChunkKey)) {
                player.sendMessage("§cYou cannot unclaim the chunk containing your Town Core!");
                return;
            }
        }

        claims.remove(chunkKey);

        Main.logTownAction(townName, player.getName() + " unclaimed chunk " + chunkKey + ".");
        saveTownData();

        player.sendMessage("§aChunk unclaimed! §f(" + claims.size()
                + "/" + Main.getMaxChunks(Main.townLevel.getOrDefault(townName, 1)) + ")");
        playUnclaimSound(player);
    }

    private boolean isClaimAdjacentToTown(String townName, Chunk chunk) {
        Set<String> claims = Main.townChunks.getOrDefault(townName, Collections.emptySet());

        if (claims.isEmpty()) {
            return true;
        }

        String worldName = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();

        String north = worldName + ":" + x + "," + (z - 1);
        String south = worldName + ":" + x + "," + (z + 1);
        String east = worldName + ":" + (x + 1) + "," + z;
        String west = worldName + ":" + (x - 1) + "," + z;

        return claims.contains(north)
                || claims.contains(south)
                || claims.contains(east)
                || claims.contains(west);
    }

    private void handleLevel(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        int townLevel = Main.townLevel.getOrDefault(townName, 1);
        int maxChunks = Main.getMaxChunks(townLevel);

        player.sendMessage("§6§l--- " + townName + " Level Info ---");
        player.sendMessage("§eTown Level: §f" + townLevel);
        player.sendMessage("§eMax Chunks: §f" + maxChunks);
        player.sendMessage("§eMob Toggle: " + getMobStatusLine(townName));
        player.sendMessage(" ");
        player.sendMessage("§6§lTask Levels:");
        player.sendMessage("§7Hover over a task to see XP progress.");

        for (String task : TownLevelManager.TASKS) {
            int taskLevel = TownLevelManager.getTaskLevel(townName, task);
            int taskXP = TownLevelManager.getTaskXP(townName, task);
            int xpNeeded = getRequiredXPForTask(task);
            int remainingXP = Math.max(0, xpNeeded - taskXP);

            Component hoverText = Component.text()
                    .append(Component.text(formatTaskName(task))
                            .color(TextColor.color(0xF4A261))
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text("\nCurrent XP: ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(taskXP)
                            .color(TextColor.color(0xFFFFFF)))
                    .append(Component.text("\nXP Needed: ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(xpNeeded)
                            .color(TextColor.color(0xFFFFFF)))
                    .append(Component.text("\nRemaining XP: ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(remainingXP)
                            .color(TextColor.color(0xFFFFFF)))
                    .append(Component.text("\nProgress: ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(taskXP + "/" + xpNeeded)
                            .color(TextColor.color(0x52B788)))
                    .build();

            Component line = Component.text("Ã¢â‚¬Â¢ ")
                    .color(TextColor.color(0x777777))
                    .append(Component.text(formatTaskName(task))
                            .color(TextColor.color(0xF4A261))
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" = ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text("Level " + taskLevel)
                            .color(TextColor.color(0x52B788))
                            .decoration(TextDecoration.BOLD, true))
                    .hoverEvent(HoverEvent.showText(hoverText));

            player.sendMessage(line);
        }
    }

    private void handleMap(Player player) {
        String playerTown = getTown(player);

        if (playerTown == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        Chunk playerChunk = player.getLocation().getChunk();
        String worldName = playerChunk.getWorld().getName();

        int centerX = playerChunk.getX();
        int centerZ = playerChunk.getZ();

        int radius = 3;

        player.sendMessage("§6§l--- Town Map ---");
        player.sendMessage("§7Legend: §aH §7= Home, §bA §7= Ally, §cE §7= Enemy, §6H* §7= Hostile, §4W §7= War, §eN §7= Neutral, §8. §7= Wilderness, §fX §7= You");

        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            StringBuilder row = new StringBuilder();

            for (int x = centerX - radius; x <= centerX + radius; x++) {
                String chunkKey = worldName + ":" + x + "," + z;

                if (x == centerX && z == centerZ) {
                    row.append("§f[X]");
                    continue;
                }

                String ownerTown = getTownAtChunk(chunkKey);

                if (ownerTown == null) {
                    row.append("§8[.]");
                    continue;
                }

                if (ownerTown.equalsIgnoreCase(playerTown)) {
                    row.append("§a[H]");
                } else if (Main.townWars.getOrDefault(playerTown, new HashSet<>()).contains(ownerTown)) {
                    row.append("§4[W]");
                } else if (Main.townAllies.getOrDefault(playerTown, new HashSet<>()).contains(ownerTown)) {
                    row.append("§b[A]");
                } else if (Main.townEnemies.getOrDefault(playerTown, new HashSet<>()).contains(ownerTown)) {
                    row.append("§c[E]");
                } else if (Main.townEnemies.getOrDefault(ownerTown, new HashSet<>()).contains(playerTown)) {
                    row.append("§6[H]");
                } else {
                    row.append("§e[N]");
                }
            }

            player.sendMessage(row.toString());
        }

        player.sendMessage("§7Center: §fYour current chunk §7(" + centerX + ", " + centerZ + ")");
    }

    private void handleBorder(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        TownBorderManager.showTownBorders(player, townName, plugin);
    }

    private void handleTitle(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /town title <player> <title>");
            player.sendMessage("§7Example: §f/town title Steve &#55FFAAGuardian");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can set town titles!");
            return;
        }

        UUID targetUUID = findTownMemberUUID(townName, args[1]);

        if (targetUUID == null) {
            player.sendMessage("§cThat player is not in your town!");
            return;
        }

        String setterRole = Main.playerRole.getOrDefault(player.getUniqueId(), "member");
        String targetRole = Main.playerRole.getOrDefault(targetUUID, "member");

        if (setterRole.equalsIgnoreCase("assistant") && targetRole.equalsIgnoreCase("ruler")) {
            player.sendMessage("§cAssistants cannot change the ruler's title!");
            return;
        }

        String title = getTownNameFromArgs(args, 2).trim();

        if (title.equalsIgnoreCase("clear") || title.equalsIgnoreCase("none")) {
            Main.playerTownTitle.remove(targetUUID);

            Main.logTownAction(townName, player.getName() + " cleared " + getOfflineName(targetUUID) + "'s town title.");
            saveTownData();

            player.sendMessage("§aCleared §f" + getOfflineName(targetUUID) + "§a's town title.");
            return;
        }

        String plainTitle = ChatListener.stripColorCodes(title);

        if (plainTitle.length() > 20) {
            player.sendMessage("§cTown titles can only be up to §f20 characters §cwithout color codes.");
            player.sendMessage("§7Your title length: §f" + plainTitle.length());
            return;
        }

        if (plainTitle.isBlank()) {
            player.sendMessage("§cTitle cannot be empty!");
            return;
        }

        Main.playerTownTitle.put(targetUUID, title);

        Main.logTownAction(townName, player.getName() + " set " + getOfflineName(targetUUID) + "'s title to " + plainTitle + ".");
        saveTownData();

        player.sendMessage("§aSet §f" + getOfflineName(targetUUID) + "§a's title to §7["
                + ChatListener.translateColors(title) + "§7]§a.");

        Player target = Bukkit.getPlayer(targetUUID);

        if (target != null && target.isOnline()) {
            target.sendMessage("§6§l[Town] §r§eYour town title was set to §7["
                    + ChatListener.translateColors(title) + "§7]§e.");
        }
    }

    private String getTownAtChunk(String chunkKey) {
        for (Map.Entry<String, Set<String>> entry : Main.townChunks.entrySet()) {
            if (entry.getValue().contains(chunkKey)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void handleTop(Player player) {
        TownTopGUI.open(player);
    }

    private void handleInspect(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town inspect <town>");
            return;
        }

        String townName = getTownNameFromArgs(args, 1);
        String realTownName = findTownName(townName);

        if (realTownName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        sendStaffTownInspect(player, realTownName);
    }

    public static void sendTownInfoToChat(Player player, String townName) {
        double balance = Main.townBank.getOrDefault(townName, 0.0);
        int memberCount = Main.townMembers.getOrDefault(townName, java.util.Collections.emptySet()).size();
        int claimsCount = Main.townChunks.getOrDefault(townName, java.util.Collections.emptySet()).size();

        String ownerName = getRulerName(townName);
        String assistantName = getAssistantName(townName);

        int allyCount = Main.townAllies.getOrDefault(townName, java.util.Collections.emptySet()).size();
        int enemyCount = Main.townEnemies.getOrDefault(townName, java.util.Collections.emptySet()).size();
        java.util.Set<String> wars = Main.townWars.getOrDefault(townName, java.util.Collections.emptySet());

        double upkeep = Main.getDailyUpkeepCost();
        String foundingDate = Main.townFoundingDate.getOrDefault(townName, "Unknown");

        player.sendMessage(" ");
        player.sendMessage("§8§m       §r §6§l" + townName.toUpperCase() + " §8§m       ");
        player.sendMessage("  §7Established on §f" + foundingDate);
        player.sendMessage(" ");

        if (WarManager.isOccupied(townName)) {
            player.sendMessage("  §c§l⚑ OCCUPIED BY " + WarManager.getOccupier(townName).toUpperCase());
        }

        long shieldExpiry = Main.townShieldExpiry.getOrDefault(townName, 0L);
        if (shieldExpiry > System.currentTimeMillis()) {
            long remaining = shieldExpiry - System.currentTimeMillis();
            long hours = remaining / 3600000;
            player.sendMessage("  §a§l✅ PEACE SHIELD §7(" + hours + "h remaining)");
        }

        // Section: Finances
        Component financeHeader = Component.text("  §6§l✦ §e§lFinancials")
                .hoverEvent(HoverEvent.showText(Component.text("§7Bank Balance: §a$" + String.format("%.2f", balance) + "\n§7Daily Upkeep: §c$" + String.format("%.2f", upkeep))));
        player.sendMessage(financeHeader);
        player.sendMessage("    §7Bank: §a$" + String.format("%.2f", balance) + " §8| §7Upkeep: §c$" + String.format("%.2f", upkeep));
        player.sendMessage(" ");

        // Section: Leadership
        player.sendMessage("  §6§l✦ §e§lLeadership");
        player.sendMessage("    §7Ruler: §f" + ownerName);
        player.sendMessage("    §7Assistant: §f" + assistantName);
        player.sendMessage(" ");

        // Section: Statistics
        player.sendMessage("  §6§l✦ §e§lStatistics");
        Component membersComp = Component.text("    §7Population: §f" + memberCount + " members")
                .hoverEvent(HoverEvent.showText(Component.text("§7Click to view member list")))
                .clickEvent(ClickEvent.runCommand("/town members " + townName));
        player.sendMessage(membersComp);
        player.sendMessage("    §7Land Size: §f" + claimsCount + " chunks");
        player.sendMessage(" ");

        // Section: Diplomacy
        player.sendMessage("  §6§l✦ §e§lDiplomacy");
        Component alliesComp = Component.text("    §7Allies: §b" + allyCount + " towns")
                .hoverEvent(HoverEvent.showText(Component.text("§7Click to see allies")))
                .clickEvent(ClickEvent.runCommand("/town allylist " + townName));
        player.sendMessage(alliesComp);

        Component enemiesComp = Component.text("    §7Enemies: §c" + enemyCount + " towns")
                .hoverEvent(HoverEvent.showText(Component.text("§7Click to see enemies")))
                .clickEvent(ClickEvent.runCommand("/town enemylist " + townName));
        player.sendMessage(enemiesComp);

        if (!wars.isEmpty()) {
            player.sendMessage("    §c§lWar Status: §f" + String.join(", ", wars));
        }

        player.sendMessage(" ");
        player.sendMessage("§8§mÃ¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬Ã¢â€“Â¬");
    }

    private Component getMobStatusComponent(String townName) {
        boolean enabled = Main.townMobsEnabled.getOrDefault(townName, true);
        if (enabled) {
            return Component.text("Enabled").color(TextColor.color(0x52b788));
        } else {
            return Component.text("Disabled").color(TextColor.color(0xe63946));
        }
    }

    private void handleAllyList(Player player, String[] args) {
        String town;
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            town = findTownName(input);
            if (town == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        } else {
            town = getTown(player);
            if (town == null) {
                player.sendMessage("§cYou don't have a town!");
                return;
            }
        }
        Set<String> allies = Main.townAllies.getOrDefault(town, Collections.emptySet());
        player.sendMessage(" ");
        player.sendMessage(Component.text("  ✦ Ally List (" + allies.size() + ") ✦")
                .color(TextColor.color(0x52b788))
                .decoration(TextDecoration.BOLD, true));
        if (allies.isEmpty()) {
            player.sendMessage("§7    None");
        } else {
            for (String ally : allies) {
                player.sendMessage(Component.text("    Ã¢â‚¬Â¢ ")
                        .color(TextColor.color(0x888888))
                        .append(Component.text(ally)
                                .color(TextColor.color(0xFFFFFF))
                                .hoverEvent(HoverEvent.showText(Component.text("§7Click for info")))
                                .clickEvent(ClickEvent.runCommand("/town inspect " + ally))));
            }
        }
        player.sendMessage(" ");
    }

    private void handleEnemyList(Player player, String[] args) {
        String town;
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            town = findTownName(input);
            if (town == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        } else {
            town = getTown(player);
            if (town == null) {
                player.sendMessage("§cYou don't have a town!");
                return;
            }
        }
        Set<String> enemies = Main.townEnemies.getOrDefault(town, Collections.emptySet());
        player.sendMessage(" ");
        player.sendMessage(Component.text("  ✦ Enemy List (" + enemies.size() + ") ✦")
                .color(TextColor.color(0xe63946))
                .decoration(TextDecoration.BOLD, true));
        if (enemies.isEmpty()) {
            player.sendMessage("§7    None");
        } else {
            for (String enemy : enemies) {
                player.sendMessage(Component.text("    Ã¢â‚¬Â¢ ")
                        .color(TextColor.color(0x888888))
                        .append(Component.text(enemy)
                                .color(TextColor.color(0xFFFFFF))
                                .hoverEvent(HoverEvent.showText(Component.text("§7Click for info")))
                                .clickEvent(ClickEvent.runCommand("/town inspect " + enemy))));
            }
        }
        player.sendMessage(" ");
    }

    private void handleHostileList(Player player, String[] args) {
        String town;
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            town = findTownName(input);
            if (town == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        } else {
            town = getTown(player);
            if (town == null) {
                player.sendMessage("§cYou don't have a town!");
                return;
            }
        }
        Set<String> hostile = getTownsThatEnemy(town);
        player.sendMessage(" ");
        player.sendMessage(Component.text("  ✦ Hostile List (" + hostile.size() + ") ✦")
                .color(TextColor.color(0xf4a261))
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  (Towns that marked you as an enemy)")
                .color(TextColor.color(0x888888)));
        if (hostile.isEmpty()) {
            player.sendMessage("§7    None");
        } else {
            for (String h : hostile) {
                player.sendMessage(Component.text("    Ã¢â‚¬Â¢ ")
                        .color(TextColor.color(0x888888))
                        .append(Component.text(h)
                                .color(TextColor.color(0xFFFFFF))
                                .hoverEvent(HoverEvent.showText(Component.text("§7Click for info")))
                                .clickEvent(ClickEvent.runCommand("/town inspect " + h))));
            }
        }
        player.sendMessage(" ");
    }

    private void sendStaffTownInspect(Player player, String townName) {
        int level = Main.townLevel.getOrDefault(townName, 1);
        int chunks = Main.townChunks.getOrDefault(townName, Collections.emptySet()).size();
        int maxChunks = Main.getMaxChunks(level);
        double balance = Main.townBank.getOrDefault(townName, 0.0);

        UUID ownerUUID = Main.townOwner.get(townName);
        UUID assistantUUID = Main.townAssistant.get(townName);

        Location spawn = Main.townSpawn.get(townName);
        Location core = Main.townCoreLocation.get(townName);

        Set<UUID> members = Main.townMembers.getOrDefault(townName, Collections.emptySet());
        Set<String> claims = Main.townChunks.getOrDefault(townName, Collections.emptySet());
        Set<String> allies = Main.townAllies.getOrDefault(townName, Collections.emptySet());
        Set<String> enemies = Main.townEnemies.getOrDefault(townName, Collections.emptySet());
        Set<String> hostile = getTownsThatEnemy(townName);
        Set<String> wars = Main.townWars.getOrDefault(townName, Collections.emptySet());

        player.sendMessage("§d§l--- Staff Town Inspect: " + townName + " ---");
        player.sendMessage("§eLevel: §f" + level);
        player.sendMessage("§eClaims: §f" + chunks + "/" + maxChunks);
        player.sendMessage("§eBank: §f$" + String.format("%.2f", balance));
        player.sendMessage("§eDaily Upkeep: §f$" + String.format("%.2f", Main.getDailyUpkeepCost()));
        player.sendMessage("§eSurvival Days: §f" + Main.getTownSurvivalDays(townName));
        player.sendMessage("§eRuler: §f" + getRulerName(townName) + " §7(" + formatUUID(ownerUUID) + ")");
        player.sendMessage("§eAssistant: §f" + getAssistantName(townName) + " §7(" + formatUUID(assistantUUID) + ")");
        player.sendMessage("§eMembers: §f" + members.size());
        player.sendMessage("§eAllies: §a" + formatTownSet(allies));
        player.sendMessage("§eEnemies: §c" + formatTownSet(enemies));
        player.sendMessage("§eHostile: §6" + formatTownSet(hostile));
        player.sendMessage("§eWars: §4" + formatTownSet(wars));
        player.sendMessage("§eMOTD: §f" + Main.townMotd.getOrDefault(townName, "Not set"));
        player.sendMessage("§eRename Used: §f" + Main.townRenameUsed.getOrDefault(townName, false));
        player.sendMessage("§eExplosions: §cAlways Blocked");
        player.sendMessage("§eHostile Mobs: " + getMobStatusLine(townName));
        player.sendMessage("§eAudit Logs: §f" + Main.townAuditLogs.getOrDefault(townName, Collections.emptyList()).size());
        player.sendMessage("§eLocked Blocks: §f" + Main.lockedBlocks.getOrDefault(townName, Collections.emptySet()).size());
        player.sendMessage("§eSpawn: §f" + formatLocation(spawn));
        player.sendMessage("§eCore: §f" + formatLocation(core));

        if (!claims.isEmpty()) {
            player.sendMessage("§eFirst Claims:");
            int shown = 0;

            for (String claim : claims) {
                if (shown >= 8) break;
                player.sendMessage("§7- §f" + claim);
                shown++;
            }

            if (claims.size() > shown) {
                player.sendMessage("§7...and §f" + (claims.size() - shown) + " §7more.");
            }
        }

        WarManager.War war = WarManager.getWarByTown(townName);

        if (war != null) {
            player.sendMessage("§4§lWar:");
            player.sendMessage("§eWar ID: §f" + WarManager.getWarId(war));
            player.sendMessage("§cAttacker: §f" + war.attackerTown + " §7Points: §f" + war.attackerPoints);
            player.sendMessage("§aDefender: §f" + war.defenderTown + " §7Points: §f" + war.defenderPoints);
            player.sendMessage("§eActive Session: §f" + war.activeSession);
            player.sendMessage("§eCore Health: §f" + WarManager.getCoreHealthPercent(war.defenderTown) + "%");
        }
    }

    private void handleBackup(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        String townName = "global";
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            townName = findTownName(input);
            if (townName == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        String backupName = StorageManager.createBackup(plugin, townName);

        if (backupName == null) {
            player.sendMessage("§cFailed to create " + townName + " backup. Check console for errors.");
            return;
        }

        player.sendMessage("§a" + (townName.equals("global") ? "Global" : townName) + " backup created: §f" + backupName);
    }

    private void handleBackups(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        String townName = "global";
        if (args.length > 1) {
            String input = getTownNameFromArgs(args, 1);
            if (input.equalsIgnoreCase("automatic") || input.equalsIgnoreCase("auto")) {
                townName = "automatic";
            } else {
                townName = findTownName(input);
                if (townName == null) {
                    player.sendMessage("§cThat town does not exist! (Use 'global' or 'automatic' for general backups)");
                    return;
                }
            }
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        List<String> backups = StorageManager.listBackups(plugin, townName);

        if (backups.isEmpty()) {
            player.sendMessage("§7No " + townName + " backups found.");
            return;
        }

        String title;
        if (townName.equals("global")) title = "Global";
        else if (townName.equals("automatic")) title = "Automatic";
        else title = townName;

        player.sendMessage("§d§l--- " + title + " Backups ---");

        int shown = 0;

        for (String backup : backups) {
            if (shown >= 10) break;

            player.sendMessage(
                    Component.text("§e" + backup + " ")
                            .append(Component.text("[Confirm Restore]")
                                    .color(TextColor.color(0xe63946))
                                    .decoration(TextDecoration.BOLD, true)
                                    .clickEvent(ClickEvent.runCommand("/town restore " + backup)))
            );

            shown++;
        }

        if (backups.size() > 10) {
            player.sendMessage("§7Showing 10 newest backups out of §f" + backups.size() + "§7.");
        }
    }

    private void handleRestore(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town restore <backupName>");
            return;
        }

        String backupName = args[1];

        if (!backupName.endsWith(".yml")) {
            backupName = backupName + ".yml";
        }

        if (backupName.contains("/") || backupName.contains("\\") || backupName.contains("..")) {
            player.sendMessage("§cInvalid backup name.");
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);

        if (!StorageManager.backupExists(plugin, backupName)) {
            player.sendMessage("§cThat backup does not exist.");
            return;
        }

        addValueConfirmation(restoreConfirmations, player, backupName);

        player.sendMessage("§c§lWARNING: §r§cRestoring a backup will replace current town data.");
        player.sendMessage("§7Backup: §f" + backupName);
        player.sendMessage("§7This confirmation expires in §f60 seconds§7.");
        player.sendMessage(
                Component.text("[âœ” Confirm Restore]")
                        .color(TextColor.color(0xe63946))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town confirmrestore " + backupName))
                        .append(Component.text("  "))
                        .append(Component.text("[âœ– Cancel]")
                                .color(TextColor.color(0x52b788))
                                .decoration(TextDecoration.BOLD, true)
                                .clickEvent(ClickEvent.runCommand("/town backups")))
        );
    }

    private void handleConfirmRestore(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town confirmrestore <backupName>");
            return;
        }

        String backupName = args[1];

        if (!backupName.endsWith(".yml")) {
            backupName = backupName + ".yml";
        }

        if (!hasValidValueConfirmation(restoreConfirmations, player, backupName, "restore")) {
            return;
        }

        if (backupName.contains("/") || backupName.contains("\\") || backupName.contains("..")) {
            player.sendMessage("§cInvalid backup name.");
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);

        WarBossBarManager.stopAllWarBossBars();
        WarManager.shutdownWarSystem();

        boolean restored = StorageManager.restoreBackup(plugin, backupName);

        if (!restored) {
            player.sendMessage("§cFailed to restore backup. Make sure the backup exists.");
            return;
        }

        WarManager.startAutomaticWarScheduler(plugin);

        Bukkit.broadcastMessage("§d§l[Staff] §r§f" + player.getName()
                + " §7restored town backup §f" + backupName + "§7.");
    }

    private void handleSave(Player player) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        StorageManager.saveData(plugin);


        player.sendMessage("§aTown data saved.");
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);

        WarBossBarManager.stopAllWarBossBars();
        WarManager.shutdownWarSystem();

        StorageManager.loadData(plugin);

        WarManager.startAutomaticWarScheduler(plugin);

        player.sendMessage("§aTown data reloaded from storage.");
    }

    private void handleLeaveConfirm(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (isRuler(player)) {
            player.sendMessage("§cYou are the ruler! You cannot leave your own town.");
            return;
        }

        addSimpleConfirmation(leaveConfirmations, player);

        player.sendMessage("§eAre you sure you want to leave §f" + townName + "§e?");
        player.sendMessage("§7This confirmation expires in §f60 seconds§7.");
        player.sendMessage(
                Component.text("[âœ” Yes, leave]")
                        .color(TextColor.color(0x52b788))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town confirmleave"))
                        .append(Component.text("  "))
                        .append(Component.text("[âœ– Nevermind]")
                                .color(TextColor.color(0xe63946))
                                .decoration(TextDecoration.BOLD, true)
                                .clickEvent(ClickEvent.runCommand("/town help town")))
        );
    }

    private void handleLeave(Player player) {
        if (!hasValidSimpleConfirmation(leaveConfirmations, player, "leave")) {
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (isRuler(player)) {
            player.sendMessage("§cYou are the ruler! You cannot leave your own town.");
            return;
        }

        UUID uuid = player.getUniqueId();

        Main.townMembers.getOrDefault(townName, new HashSet<>()).remove(uuid);
        Main.playerTown.remove(uuid);
        Main.playerRole.remove(uuid);
        Main.playerTownTitle.remove(uuid);
        ChatListener.townChatPlayers.remove(uuid);
        ChatListener.allyChatPlayers.remove(uuid);

        if (uuid.equals(Main.townAssistant.get(townName))) {
            Main.townAssistant.remove(townName);
        }


        Main.logTownAction(townName, player.getName() + " left the town.");
        saveTownData();

        player.sendMessage("§aYou have left §f" + townName + "§a!");

        for (UUID memberUUID : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member != null) {
                member.sendMessage("§e§l[Town] §r§f" + player.getName() + " §7has left the town.");
            }
        }
    }

    private void handleDisbandConfirm(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRuler(player)) {
            player.sendMessage("§cOnly the ruler can disband the town!");
            return;
        }

        WarManager.War war = WarManager.getWarByTown(townName);

        if (war != null) {
            player.sendMessage("§cYou cannot disband your town while it is in a war!");
            return;
        }

        addSimpleConfirmation(disbandConfirmations, player);

        player.sendMessage("§c§lWARNING: §r§cThis will permanently delete §f" + townName + "§c.");
        player.sendMessage("§7This removes all claims, members, bank money, spawn, relations, logs, and town data.");
        player.sendMessage("§7This confirmation expires in §f60 seconds§7.");
        player.sendMessage(
                Component.text("[âœ” Yes, disband town]")
                        .color(TextColor.color(0xe63946))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town confirmdisband"))
                        .append(Component.text("  "))
                        .append(Component.text("[âœ– Cancel]")
                                .color(TextColor.color(0x52b788))
                                .decoration(TextDecoration.BOLD, true)
                                .clickEvent(ClickEvent.runCommand("/town help town")))
        );
    }

    private void handleDisband(Player player) {
        if (!hasValidSimpleConfirmation(disbandConfirmations, player, "disband")) {
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRuler(player)) {
            player.sendMessage("§cOnly the ruler can disband the town!");
            return;
        }

        WarManager.War war = WarManager.getWarByTown(townName);

        if (war != null) {
            player.sendMessage("§cYou cannot disband your town while it is in a war!");
            return;
        }

        disbandTown(townName, player.getName(), false);
    }

    private void disbandTown(String townName, String actorName, boolean adminDisband) {
        Set<UUID> members = new HashSet<>(Main.townMembers.getOrDefault(townName, Collections.emptySet()));

        Main.logTownAction(townName, actorName + " disbanded the town.");


        for (UUID memberUUID : members) {
            Main.playerTown.remove(memberUUID);
            Main.playerRole.remove(memberUUID);
            Main.playerTownTitle.remove(memberUUID);
            ChatListener.townChatPlayers.remove(memberUUID);
            ChatListener.allyChatPlayers.remove(memberUUID);

            Player member = Bukkit.getPlayer(memberUUID);

            if (member != null) {
                if (adminDisband) {
                    member.sendMessage("§c§l[Town Disbanded] §r§f" + townName + " §chas been force-disbanded by staff.");
                } else {
                    member.sendMessage("§c§l[Town Disbanded] §r§f" + townName + " §chas been disbanded by §f" + actorName + "§c.");
                }
            }
        }

        WarManager.War war = WarManager.getWarByTown(townName);
        if (war != null) {
            WarManager.removeWar(war);
        }

        Main.townLevel.remove(townName);
        Main.townBank.remove(townName);
        Main.townChunks.remove(townName);
        Main.townOwner.remove(townName);
        Main.townAssistant.remove(townName);
        Main.townMembers.remove(townName);
        Main.townAllies.remove(townName);
        Main.townWars.remove(townName);
        Main.townEnemies.remove(townName);
        Main.pendingAllyRequests.remove(townName);
        Main.townSpawn.remove(townName);
        Main.townCoreLocation.remove(townName);
        Main.townSpawnCooldown.remove(townName);
        Main.townRenameUsed.remove(townName);
        Main.pendingRenames.remove(townName);
        Main.pendingRenameRequester.remove(townName);
        Main.townMotd.remove(townName);
        Main.lockedBlocks.remove(townName);
        Main.townMobsEnabled.remove(townName);
        Main.townAuditLogs.remove(townName);

        Main.townPublicJoin.remove(townName);

        Main.townChatFormat.remove(townName);

        TownLevelManager.townTaskXP.remove(townName);
        TownLevelManager.townTaskLevel.remove(townName);

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        StorageManager.deleteTownBackups(plugin, townName);

        for (Set<String> allies : Main.townAllies.values()) {
            allies.remove(townName);
        }

        for (Set<String> enemies : Main.townEnemies.values()) {
            enemies.remove(townName);
        }

        for (Set<String> wars : Main.townWars.values()) {
            wars.remove(townName);
        }

        for (Set<String> requests : Main.pendingAllyRequests.values()) {
            requests.remove(townName);
        }

        LockManager.lockedBlockOwners.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return key != null && key.startsWith(townName + ":");
        });

        saveTownData();

        if (adminDisband) {
            Bukkit.broadcastMessage("§c§l[Town] §r§f" + townName + " §chas been force-disbanded by staff.");
        } else {
            Bukkit.broadcastMessage("§c§l[Town] §r§f" + townName + " §chas been disbanded by §f" + actorName + "§c!");
        }
    }

    private void handleKickConfirm(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town kick <player>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can kick members!");
            return;
        }

        String targetName = args[1];
        addValueConfirmation(kickConfirmations, player, targetName);

        player.sendMessage("§eAre you sure you want to kick §f" + targetName + "§e from your town?");
        player.sendMessage("§7This confirmation expires in §f60 seconds§7.");
        player.sendMessage(
                Component.text("[âœ” Yes, kick]")
                        .color(TextColor.color(0x52b788))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town confirmkick " + targetName))
                        .append(Component.text("  "))
                        .append(Component.text("[âœ– Nevermind]")
                                .color(TextColor.color(0xe63946))
                                .decoration(TextDecoration.BOLD, true)
                                .clickEvent(ClickEvent.runCommand("/town help members")))
        );
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town confirmkick <player>");
            return;
        }

        if (!hasValidValueConfirmation(kickConfirmations, player, args[1], "kick")) {
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can kick members!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage("§cPlayer not found or not online!");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String targetTown = Main.playerTown.get(targetUUID);

        if (targetTown == null || !targetTown.equals(townName)) {
            player.sendMessage("§cThat player is not in your town!");
            return;
        }

        String playerRole = Main.playerRole.get(player.getUniqueId());
        String targetRole = Main.playerRole.getOrDefault(targetUUID, "member");

        if (targetRole.equals("ruler")) {
            player.sendMessage("§cYou can't kick the ruler!");
            return;
        }

        if (targetRole.equals("assistant") && playerRole.equals("assistant")) {
            player.sendMessage("§cAssistants can't kick other assistants!");
            return;
        }

        Main.townMembers.getOrDefault(townName, new HashSet<>()).remove(targetUUID);
        Main.playerTown.remove(targetUUID);
        Main.playerRole.remove(targetUUID);
        Main.playerTownTitle.remove(targetUUID);
        ChatListener.townChatPlayers.remove(targetUUID);
        ChatListener.allyChatPlayers.remove(targetUUID);

        if (Main.townAssistant.get(townName) != null && Main.townAssistant.get(townName).equals(targetUUID)) {
            Main.townAssistant.remove(townName);
        }


        Main.logTownAction(townName, player.getName() + " kicked " + target.getName() + " from the town.");
        saveTownData();

        target.sendMessage("§cYou have been kicked from §f" + townName + "§c!");
        player.sendMessage("§aKicked §f" + target.getName() + " §afrom your town!");

        for (UUID memberUUID : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member != null && !member.equals(player)) {
                member.sendMessage("§e§l[Town] §r§f" + target.getName() + " §7was kicked from the town.");
            }
        }
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town promote <player>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRuler(player)) {
            player.sendMessage("§cOnly the ruler can promote members!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage("§cPlayer not found or not online!");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String targetTown = Main.playerTown.get(targetUUID);

        if (targetTown == null || !targetTown.equals(townName)) {
            player.sendMessage("§cThat player is not in your town!");
            return;
        }

        if (Main.playerRole.getOrDefault(targetUUID, "member").equals("assistant")) {
            player.sendMessage("§cThat player is already an assistant!");
            return;
        }

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage("§cYou can't promote yourself!");
            return;
        }

        Main.playerRole.put(targetUUID, "assistant");
        Main.townAssistant.put(townName, targetUUID);


        Main.logTownAction(townName, player.getName() + " promoted " + target.getName() + " to assistant.");
        saveTownData();

        target.sendMessage("§aYou have been promoted to Assistant in §f" + townName + "§a!");
        player.sendMessage("§aPromoted §f" + target.getName() + " §ato Assistant!");
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town demote <player>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRuler(player)) {
            player.sendMessage("§cOnly the ruler can demote members!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage("§cPlayer not found or not online!");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String targetTown = Main.playerTown.get(targetUUID);

        if (targetTown == null || !targetTown.equals(townName)) {
            player.sendMessage("§cThat player is not in your town!");
            return;
        }

        if (!Main.playerRole.getOrDefault(targetUUID, "member").equals("assistant")) {
            player.sendMessage("§cThat player is not an assistant!");
            return;
        }

        Main.playerRole.put(targetUUID, "member");
        Main.townAssistant.remove(townName);


        Main.logTownAction(townName, player.getName() + " demoted " + target.getName() + " to member.");
        saveTownData();

        target.sendMessage("§cYou have been demoted to Member in §f" + townName + "§c!");
        player.sendMessage("§aDemoted §f" + target.getName() + " §ato Member!");
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage:");
            player.sendMessage("§7/town ally <town>");
            player.sendMessage("§7/town ally accept <town>");
            player.sendMessage("§7/town ally deny <town>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can manage allies!");
            return;
        }

        if (args[1].equalsIgnoreCase("accept")) {
            handleAllyAccept(player, args, townName);
            return;
        }

        if (args[1].equalsIgnoreCase("deny")) {
            handleAllyDeny(player, args, townName);
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String targetTown = findTownName(inputTown);

        if (targetTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (townName.equalsIgnoreCase(targetTown)) {
            player.sendMessage("§cYou can't ally yourself!");
            return;
        }

        if (Main.townAllies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
            player.sendMessage("§cYou are already allied with §f" + targetTown + "§c!");
            return;
        }

        if (Main.townWars.getOrDefault(townName, Collections.emptySet()).contains(targetTown)
                || Main.townWars.getOrDefault(targetTown, Collections.emptySet()).contains(townName)) {
            player.sendMessage("§cYou cannot send an ally request to a town you are at war with!");
            return;
        }

        if (Main.townEnemies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)
                || Main.townEnemies.getOrDefault(targetTown, Collections.emptySet()).contains(townName)) {
            player.sendMessage("§cYou must remove the enemy/hostile relation first!");
            return;
        }

        if (getAllyCount(townName) >= MAX_ALLIES) {
            player.sendMessage("§cYour town already has the max amount of allies! §7(" + MAX_ALLIES + ")");
            return;
        }

        if (getAllyCount(targetTown) >= MAX_ALLIES) {
            player.sendMessage("§cThat town already has the max amount of allies! §7(" + MAX_ALLIES + ")");
            return;
        }

        if (Main.pendingAllyRequests.getOrDefault(targetTown, Collections.emptySet()).contains(townName)) {
            player.sendMessage("§cYou already sent an ally request to §f" + targetTown + "§c!");
            return;
        }

        if (Main.pendingAllyRequests.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
            player.sendMessage("§eThat town already sent your town an ally request.");
            player.sendMessage("§7Use §a/town ally accept " + targetTown + " §7or §c/town ally deny " + targetTown);
            return;
        }

        Main.pendingAllyRequests.putIfAbsent(targetTown, new HashSet<>());
        Main.pendingAllyRequests.get(targetTown).add(townName);

        Main.logTownAction(townName, player.getName() + " sent an ally request to " + targetTown + ".");
        Main.logTownAction(targetTown, townName + " sent this town an ally request.");
        saveTownData();

        player.sendMessage("§aAlly request sent to §f" + targetTown + "§a!");
        notifyTownAllyRequest(targetTown, townName);
    }

    private void handleAllyAccept(Player player, String[] args, String townName) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /town ally accept <town>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 2);
        String requestTown = findTownName(inputTown);

        if (requestTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (!Main.pendingAllyRequests.getOrDefault(townName, Collections.emptySet()).contains(requestTown)) {
            player.sendMessage("§cYou do not have an ally request from §f" + requestTown + "§c!");
            return;
        }

        if (getAllyCount(townName) >= MAX_ALLIES) {
            player.sendMessage("§cYour town already has the max amount of allies! §7(" + MAX_ALLIES + ")");
            return;
        }

        if (getAllyCount(requestTown) >= MAX_ALLIES) {
            player.sendMessage("§cThat town already has the max amount of allies! §7(" + MAX_ALLIES + ")");
            return;
        }

        if (Main.townWars.getOrDefault(townName, Collections.emptySet()).contains(requestTown)
                || Main.townWars.getOrDefault(requestTown, Collections.emptySet()).contains(townName)) {
            player.sendMessage("§cYou cannot ally a town you are at war with!");
            return;
        }

        if (Main.townEnemies.getOrDefault(townName, Collections.emptySet()).contains(requestTown)
                || Main.townEnemies.getOrDefault(requestTown, Collections.emptySet()).contains(townName)) {
            player.sendMessage("§cYou must remove the enemy/hostile relation first!");
            return;
        }

        Main.pendingAllyRequests.getOrDefault(townName, new HashSet<>()).remove(requestTown);

        addTownRelation(Main.townAllies, townName, requestTown);


        Main.logTownAction(townName, player.getName() + " accepted an ally request from " + requestTown + ".");
        Main.logTownAction(requestTown, townName + " accepted this town's ally request.");
        saveTownData();

        player.sendMessage("§aYou accepted the ally request from §f" + requestTown + "§a!");
        notifyTown(townName, "§a§l[Diplomacy] §r§f" + townName + " §ais now allied with §f" + requestTown + "§a!");
        notifyTown(requestTown, "§a§l[Diplomacy] §r§f" + townName + " §aaccepted your ally request!");
    }

    private void handleAllyDeny(Player player, String[] args, String townName) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /town ally deny <town>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 2);
        String requestTown = findTownName(inputTown);

        if (requestTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (!Main.pendingAllyRequests.getOrDefault(townName, Collections.emptySet()).contains(requestTown)) {
            player.sendMessage("§cYou do not have an ally request from §f" + requestTown + "§c!");
            return;
        }

        Main.pendingAllyRequests.getOrDefault(townName, new HashSet<>()).remove(requestTown);

        Main.logTownAction(townName, player.getName() + " denied an ally request from " + requestTown + ".");
        Main.logTownAction(requestTown, townName + " denied this town's ally request.");
        saveTownData();

        player.sendMessage("§cYou denied the ally request from §f" + requestTown + "§c!");
        notifyTown(requestTown, "§c§l[Diplomacy] §r§f" + townName + " §cdenied your ally request.");
    }

    private void handleUnally(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town unally <town>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can remove allies!");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String targetTown = findTownName(inputTown);

        if (targetTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (!Main.townAllies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
            player.sendMessage("§cYou are not allied with §f" + targetTown + "§c!");
            return;
        }

        removeTownRelation(Main.townAllies, townName, targetTown);


        Main.logTownAction(townName, player.getName() + " removed ally " + targetTown + ".");
        Main.logTownAction(targetTown, townName + " removed this town as an ally.");
        saveTownData();

        player.sendMessage("§cYou removed the alliance with §f" + targetTown + "§c!");
        notifyTown(townName, "§c§l[Diplomacy] §r§f" + townName + " §cis no longer allied with §f" + targetTown + "§c!");
        notifyTown(targetTown, "§c§l[Diplomacy] §r§f" + townName + " §chas ended the alliance with your town!");
    }

    private void handleEnemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town enemy <town>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can enemy towns!");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String targetTown = findTownName(inputTown);

        if (targetTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (townName.equalsIgnoreCase(targetTown)) {
            player.sendMessage("§cYou can't enemy yourself!");
            return;
        }

        if (Main.townAllies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
            player.sendMessage("§cYou must remove the alliance with §f" + targetTown + " §cfirst!");
            return;
        }

        if (Main.townEnemies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
            player.sendMessage("§c" + targetTown + " §cis already your enemy!");
            return;
        }

        Main.townEnemies.putIfAbsent(townName, new HashSet<>());
        Main.townEnemies.get(townName).add(targetTown);

        removePendingAllyRequestBetween(townName, targetTown);


        Main.logTownAction(townName, player.getName() + " marked " + targetTown + " as an enemy.");
        Main.logTownAction(targetTown, townName + " now considers this town an enemy.");
        saveTownData();

        player.sendMessage("§cYour town now considers §f" + targetTown + " §can enemy!");
        notifyTown(targetTown, "§6§l[Diplomacy] §r§f" + townName + " §6now considers your town hostile.");

        Bukkit.broadcastMessage("§c§l[Diplomacy] §r§f" + townName
                + " §cnow considers §f" + targetTown + " §can enemy!");
    }

    private void handleUnenemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town unenemy <town>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can remove enemies!");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String targetTown = findTownName(inputTown);

        if (targetTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (!Main.townEnemies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
            player.sendMessage("§cYour town does not consider §f" + targetTown + " §can enemy!");
            return;
        }

        Main.townEnemies.getOrDefault(townName, new HashSet<>()).remove(targetTown);


        Main.logTownAction(townName, player.getName() + " removed enemy " + targetTown + ".");
        Main.logTownAction(targetTown, townName + " no longer considers this town an enemy.");
        saveTownData();

        player.sendMessage("§aYour town no longer considers §f" + targetTown + " §aan enemy.");
        notifyTown(targetTown, "§a§l[Diplomacy] §r§f" + townName + " §ano longer considers your town hostile.");
    }

    private int getAllyCount(String townName) {
        return Main.townAllies.getOrDefault(townName, Collections.emptySet()).size();
    }

    private void addTownRelation(Map<String, Set<String>> relationMap, String townA, String townB) {
        relationMap.putIfAbsent(townA, new HashSet<>());
        relationMap.putIfAbsent(townB, new HashSet<>());
        relationMap.get(townA).add(townB);
        relationMap.get(townB).add(townA);
    }

    private void removeTownRelation(Map<String, Set<String>> relationMap, String townA, String townB) {
        if (relationMap.containsKey(townA)) {
            relationMap.get(townA).remove(townB);
        }

        if (relationMap.containsKey(townB)) {
            relationMap.get(townB).remove(townA);
        }
    }

    private void removePendingAllyRequestBetween(String townA, String townB) {
        if (Main.pendingAllyRequests.containsKey(townA)) {
            Main.pendingAllyRequests.get(townA).remove(townB);
        }

        if (Main.pendingAllyRequests.containsKey(townB)) {
            Main.pendingAllyRequests.get(townB).remove(townA);
        }
    }

    public static Set<String> getTownsThatEnemy(String townName) {
        Set<String> result = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : Main.townEnemies.entrySet()) {
            String otherTown = entry.getKey();
            Set<String> otherEnemies = entry.getValue();

            if (otherTown.equalsIgnoreCase(townName)) {
                continue;
            }

            if (otherEnemies != null && otherEnemies.contains(townName)) {
                result.add(otherTown);
            }
        }

        return result;
    }

    private void notifyTown(String townName, String message) {
        for (UUID uuid : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            Player member = Bukkit.getPlayer(uuid);

            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    private void notifyTownAllyRequest(String targetTown, String requestTown) {
        Component title = Component.text("§a§l[Ally Request] ")
                .append(Component.text(requestTown + " wants to ally your town!")
                        .color(TextColor.color(0xA7F3D0))
                        .decoration(TextDecoration.BOLD, false));

        Component acceptButton = Component.text("[ACCEPT]")
                .color(TextColor.color(0x52B788))
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/town ally accept " + requestTown))
                .hoverEvent(HoverEvent.showText(Component.text("Accept ally request from " + requestTown)));

        Component denyButton = Component.text("[DENY]")
                .color(TextColor.color(0xE63946))
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/town ally deny " + requestTown))
                .hoverEvent(HoverEvent.showText(Component.text("Deny ally request from " + requestTown)));

        Component buttons = acceptButton
                .append(Component.text(" "))
                .append(denyButton);

        UUID ownerUUID = Main.townOwner.get(targetTown);
        UUID assistantUUID = Main.townAssistant.get(targetTown);

        boolean sent = false;

        if (ownerUUID != null) {
            Player owner = Bukkit.getPlayer(ownerUUID);

            if (owner != null && owner.isOnline()) {
                owner.sendMessage(title);
                owner.sendMessage(buttons);
                sent = true;
            }
        }

        if (assistantUUID != null && !assistantUUID.equals(ownerUUID)) {
            Player assistant = Bukkit.getPlayer(assistantUUID);

            if (assistant != null && assistant.isOnline()) {
                assistant.sendMessage(title);
                assistant.sendMessage(buttons);
                sent = true;
            }
        }

        if (!sent) {
            for (UUID uuid : Main.townMembers.getOrDefault(targetTown, Collections.emptySet())) {
                Player member = Bukkit.getPlayer(uuid);

                if (member != null && member.isOnline()) {
                    member.sendMessage("§a§l[Ally Request] §r§f" + requestTown + " §ahas sent your town an ally request.");
                    member.sendMessage("§7Tell your ruler or assistant to use §a/town ally accept " + requestTown
                            + " §7or §c/town ally deny " + requestTown);
                }
            }
        }
    }

    private void handleDeclareWarConfirm(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town declarewar <town>");
            return;
        }

        String attackerTown = getTown(player);

        if (attackerTown == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can declare war!");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String defenderTown = findTownName(inputTown);

        if (defenderTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (!Main.townEnemies.getOrDefault(attackerTown, new HashSet<>()).contains(defenderTown)) {
            player.sendMessage("§cYou must mark §f" + defenderTown + " §cas an enemy first!");
            return;
        }

        if (Main.townAllies.getOrDefault(attackerTown, new HashSet<>()).contains(defenderTown)) {
            player.sendMessage("§cYou cannot declare war on an ally!");
            return;
        }

        addValueConfirmation(warConfirmations, player, defenderTown);

        player.sendMessage("§eAre you sure you want to declare war on §f" + defenderTown + "§e?");
        player.sendMessage("§7This confirmation expires in §f60 seconds§7.");
        player.sendMessage(
                Component.text("[Ã¢Å¡â€ Yes, declare war]")
                        .color(TextColor.color(0xe63946))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town confirmdeclarewar " + defenderTown))
                        .append(Component.text("  "))
                        .append(Component.text("[âœ– Nevermind]")
                                .color(TextColor.color(0x52b788))
                                .decoration(TextDecoration.BOLD, true)
                                .clickEvent(ClickEvent.runCommand("/town help diplomacy")))
        );
    }

    private void handleDeclareWar(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town confirmdeclarewar <town>");
            return;
        }

        String attackerTown = getTown(player);

        if (attackerTown == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String defenderTown = findTownName(inputTown);

        if (defenderTown == null) {
            player.sendMessage("§cThat town doesn't exist!");
            return;
        }

        if (!hasValidValueConfirmation(warConfirmations, player, defenderTown, "declarewar")) {
            return;
        }

        WarManager.declareWar(player, attackerTown, defenderTown);


        Main.logTownAction(attackerTown, player.getName() + " declared war on " + defenderTown + ".");
        Main.logTownAction(defenderTown, attackerTown + " declared war on this town.");
        saveTownData();
    }

    private void handleRevoltConfirm(Player player) {
        String townName = getTown(player);
        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRuler(player)) {
            player.sendMessage("§cOnly the ruler can initiate a revolt!");
            return;
        }

        if (!WarManager.isOccupied(townName)) {
            player.sendMessage("§cYour town is not occupied!");
            return;
        }

        addSimpleConfirmation(revoltConfirmations, player);

        player.sendMessage(" ");
        player.sendMessage("§c§l⚑ ATTENTION ⚑");
        player.sendMessage("§eAre you sure you want to §fREVOLT §eagainst §f" + WarManager.getOccupier(townName) + "§e?");
        player.sendMessage("§7This will immediately start a war. If you win, you will be free.");
        player.sendMessage("§7This confirmation expires in §f60 seconds§7.");
        player.sendMessage(
                Component.text("[Ã¢Å¡â€ Yes, RISE UP]")
                        .color(TextColor.color(0xe63946))
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/town confirmrevolt"))
        );
        player.sendMessage(" ");
    }

    private void handleRevolt(Player player) {
        String townName = getTown(player);
        if (townName == null) return;

        if (!hasValidSimpleConfirmation(revoltConfirmations, player, "revolt")) {
            return;
        }

        WarManager.revolt(player, townName);
        saveTownData();
    }

    private void handleWarInfo(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        WarManager.War war = WarManager.getWarByTown(townName);

        if (war == null) {
            player.sendMessage("§cYour town is not in a war!");
            return;
        }

        Location core = Main.townCoreLocation.get(war.defenderTown);
        String coreLocation = "Unknown";

        if (core != null && core.getWorld() != null) {
            coreLocation = core.getWorld().getName()
                    + " X:" + core.getBlockX()
                    + " Y:" + core.getBlockY()
                    + " Z:" + core.getBlockZ();
        }

        player.sendMessage("§4§l--- War Info ---");

        if (player.hasPermission("silvarys.staff")) {
            player.sendMessage("§eWar ID: §f" + WarManager.getWarId(war));
        }

        player.sendMessage("§cAttackers: §f" + war.attackerTown + " §7- Points: §f" + war.attackerPoints);
        if (!war.attackerAllies.isEmpty()) {
            player.sendMessage("§7Attacker Allies: §f" + String.join(", ", war.attackerAllies));
        }
        player.sendMessage("§aDefenders: §f" + war.defenderTown + " §7- Points: §f" + war.defenderPoints);
        if (!war.defenderAllies.isEmpty()) {
            player.sendMessage("§7Defender Allies: §f" + String.join(", ", war.defenderAllies));
        }
        player.sendMessage("§eActive Session: §f" + war.activeSession);
        player.sendMessage("§eCore Health: §f" + WarManager.getCoreHealthPercent(war.defenderTown) + "%");
        player.sendMessage("§eCore Location: §f" + coreLocation);
    }

    private void handleSurrender(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRuler(player)) {
            player.sendMessage("§cOnly the ruler can surrender!");
            return;
        }

        WarManager.War war = WarManager.getWarByTown(townName);

        if (war == null) {
            player.sendMessage("§cYour town is not in a war!");
            return;
        }

        boolean isAttacker = war.attackerTown.equals(townName);
        String winner = isAttacker ? war.defenderTown : war.attackerTown;
        String loser = townName;

        double loserBank = Main.townBank.getOrDefault(loser, 0.0);
        double penalty = loserBank * 0.5;

        Main.townBank.put(loser, loserBank - penalty);
        Main.townBank.put(winner, Main.townBank.getOrDefault(winner, 0.0) + penalty);

        WarManager.removeWar(war);


        Main.logTownAction(loser, player.getName() + " surrendered to " + winner + ". Lost $" + String.format("%.2f", penalty) + ".");
        Main.logTownAction(winner, loser + " surrendered. Gained $" + String.format("%.2f", penalty) + ".");
        saveTownData();

        Bukkit.broadcastMessage("§4§l[WAR] §r§f" + loser + " §chas surrendered to §f" + winner + "§c!");
        Bukkit.broadcastMessage("§c§f" + winner + " §creceived §f$" + String.format("%.2f", penalty)
                + " §cfrom the surrender!");
    }

    private void handleList(Player player) {
        TownListGUI.open(player, 1);
    }

    private void handleChat(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (ChatListener.townChatPlayers.contains(uuid)) {
            ChatListener.townChatPlayers.remove(uuid);
            player.sendMessage("§7Switched to §fGlobal Chat§7.");
        } else {
            ChatListener.townChatPlayers.add(uuid);
            ChatListener.allyChatPlayers.remove(uuid);
            player.sendMessage("§6Switched to §f" + townName
                    + " §6Town Chat§6. Type §f/town chat §6again to go back to global.");
        }
    }

    private void handleAllyChat(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (Main.townAllies.getOrDefault(townName, new HashSet<>()).isEmpty()) {
            player.sendMessage("§cYour town has no allies!");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (ChatListener.allyChatPlayers.contains(uuid)) {
            ChatListener.allyChatPlayers.remove(uuid);
            player.sendMessage("§7Switched to §fGlobal Chat§7.");
        } else {
            ChatListener.allyChatPlayers.add(uuid);
            ChatListener.townChatPlayers.remove(uuid);
            player.sendMessage("§bSwitched to §fAlly Chat§b. Type §f/town allychat §bagain to go back to global.");
        }
    }

    private void handleMotd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town motd <message>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can set the MOTD!");
            return;
        }

        String motd = getTownNameFromArgs(args, 1);
        Main.townMotd.put(townName, motd);


        Main.logTownAction(townName, player.getName() + " changed the town MOTD.");
        saveTownData();

        player.sendMessage("§aTown MOTD set to: §f" + motd);
    }

    private void handleClearMotd(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can clear the MOTD!");
            return;
        }

        Main.townMotd.remove(townName);


        Main.logTownAction(townName, player.getName() + " cleared the town MOTD.");
        saveTownData();

        player.sendMessage("§aTown MOTD cleared!");
    }

    private void handleMobs(Player player, String[] args) {
        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        int level = Main.townLevel.getOrDefault(townName, 1);

        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            player.sendMessage("§6§l--- Town Mob Settings ---");
            player.sendMessage("§eTown: §f" + townName);
            player.sendMessage("§eLevel: §f" + level);
            player.sendMessage("§eHostile Mobs: " + getMobStatusLine(townName));

            if (!TownUpgradesManager.hasUpgrade(townName, TownUpgradesManager.PERK_MOBS_TOGGLE)) {
                player.sendMessage("§7Mob toggling requires the §fPeacekeeper §7upgrade.");
            } else {
                player.sendMessage("§7Use §f/town mobs on §7or §f/town mobs off§7.");
            }

            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can change mob settings!");
            return;
        }

        if (!TownUpgradesManager.hasUpgrade(townName, TownUpgradesManager.PERK_MOBS_TOGGLE)) {
            player.sendMessage("§cYour town needs the §fPeacekeeper §cupgrade to toggle hostile mob spawning!");
            player.sendMessage("§7Unlock it in §f/town upgrades§7.");
            return;
        }

        if (args[1].equalsIgnoreCase("on")) {
            Main.townMobsEnabled.put(townName, true);
            Main.logTownAction(townName, player.getName() + " enabled hostile mob spawning.");
            saveTownData();

            player.sendMessage("§aHostile mob spawning is now enabled in your town claims.");
            return;
        }

        if (args[1].equalsIgnoreCase("off")) {
            Main.townMobsEnabled.put(townName, false);
            Main.logTownAction(townName, player.getName() + " disabled hostile mob spawning.");
            saveTownData();

            player.sendMessage("§cHostile mob spawning is now disabled in your town claims.");
            return;
        }

        player.sendMessage("§cUsage: /town mobs on/off/status");
    }

    private void handleLogs(Player player, String[] args) {
        String townName;

        if (args.length >= 2) {
            if (!player.hasPermission("silvarys.staff")) {
                player.sendMessage("§cOnly staff can view another town's logs!");
                return;
            }

            String inputTown = getTownNameFromArgs(args, 1);
            townName = findTownName(inputTown);

            if (townName == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }
        } else {
            townName = getTown(player);

            if (townName == null) {
                player.sendMessage("§cYou don't have a town!");
                return;
            }

            if (!isRulerOrAssistant(player)) {
                player.sendMessage("§cOnly the ruler or assistant can view town logs!");
                return;
            }
        }

        List<String> logs = Main.townAuditLogs.getOrDefault(townName, Collections.emptyList());

        if (logs.isEmpty()) {
            player.sendMessage("§7No audit logs recorded yet.");
            return;
        }

        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();

        if (meta != null) {
            meta.title(net.kyori.adventure.text.Component.text(townName + " Logs"));
            meta.author(net.kyori.adventure.text.Component.text("System"));

            List<net.kyori.adventure.text.Component> pages = new ArrayList<>();
            net.kyori.adventure.text.Component currentPage = net.kyori.adventure.text.Component.text("§l" + townName + " Audit Logs\n\n");
            
            int logsPerPage = 6;
            int count = 0;

            for (int i = logs.size() - 1; i >= 0; i--) {
                String log = logs.get(i);
                currentPage = currentPage.append(net.kyori.adventure.text.Component.text("§8- §0" + log + "\n\n"));
                count++;

                if (count == logsPerPage) {
                    pages.add(currentPage);
                    currentPage = net.kyori.adventure.text.Component.text("");
                    count = 0;
                }
            }

            if (count > 0) {
                pages.add(currentPage);
            }

            meta.pages(pages);
            book.setItemMeta(meta);
        }

        player.openBook(book);
    }

    private void handleUpgrades(Player player) {
        String townName = getTown(player);
        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can open town upgrades!");
            return;
        }

        TownUpgradesGUI.open(player);
    }

    private void handleCallToArms(Player player, String[] args) {
        String townName = getTown(player);
        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can call allies to arms!");
            return;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase("accept")) {
            String targetTown = findTownName(getTownNameFromArgs(args, 2));
            if (targetTown == null) {
                player.sendMessage("§cThat town does not exist!");
                return;
            }

            if (!Main.townAllies.getOrDefault(townName, Collections.emptySet()).contains(targetTown)) {
                player.sendMessage("§cYou are not allied with " + targetTown + "!");
                return;
            }

            WarManager.War war = WarManager.getWarByTown(targetTown);
            if (war == null || !war.activeSession) {
                player.sendMessage("§c" + targetTown + " is not currently in an active war session!");
                return;
            }

            if (war.attackerTown.equalsIgnoreCase(targetTown)) {
                war.attackerAllies.add(townName);
                Bukkit.broadcastMessage("§4§l[WAR] §r§f" + townName + " §chas answered the Call to Arms and joined the attackers!");
            } else if (war.defenderTown.equalsIgnoreCase(targetTown)) {
                war.defenderAllies.add(townName);
                Bukkit.broadcastMessage("§4§l[WAR] §r§f" + townName + " §chas answered the Call to Arms and joined the defenders!");
            }

            Main.logTownAction(townName, player.getName() + " accepted Call to Arms for " + targetTown + ".");
            return;
        }

        WarManager.War war = WarManager.getWarByTown(townName);
        if (war == null || !war.activeSession) {
            player.sendMessage("§cYour town is not currently in an active war session!");
            return;
        }

        Set<String> allies = Main.townAllies.getOrDefault(townName, Collections.emptySet());
        if (allies.isEmpty()) {
            player.sendMessage("§cYour town has no allies to call to arms!");
            return;
        }

        player.sendMessage("§aSent Call to Arms to all allies!");

        net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("§4§l[Call to Arms] §r§f" + townName + " §cneeds your help in their war! ")
                .append(net.kyori.adventure.text.Component.text("[ACCEPT]")
                        .color(net.kyori.adventure.text.format.TextColor.color(0x52b788))
                        .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/town calltoarms accept " + townName))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("Click to join their war side"))));

        for (String ally : allies) {
            for (UUID uuid : Main.townMembers.getOrDefault(ally, Collections.emptySet())) {
                if (Main.playerRole.getOrDefault(uuid, "member").equals("ruler") || Main.playerRole.getOrDefault(uuid, "member").equals("assistant")) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(message);
                    }
                }
            }
        }
    }


    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town rename <newname>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can rename the town!");
            return;
        }

        if (Main.townRenameUsed.getOrDefault(townName, false)) {
            player.sendMessage("§cYour town has already used its one-time rename!");
            return;
        }

        if (Main.pendingRenames.containsKey(townName)) {
            player.sendMessage("§cYou already have a pending rename request!");
            return;
        }

        String newName = getTownNameFromArgs(args, 1);

        if (findTownName(newName) != null) {
            player.sendMessage("§cA town with that name already exists!");
            return;
        }

        Main.pendingRenames.put(townName, newName);
        Main.pendingRenameRequester.put(townName, player.getName());

        Main.logTownAction(townName, player.getName() + " requested town rename to " + newName + ".");
        saveTownData();

        player.sendMessage("§aRename request submitted! §7Waiting for staff approval.");
        player.sendMessage("§7Requested name: §f" + newName);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("silvarys.staff")) {
                online.sendMessage("§6§l[Staff] §r§f" + player.getName()
                        + " §7requested to rename §f" + townName + " §7to §f" + newName);
                online.sendMessage("§7Use §f/town renamerequest §7to see all pending requests.");
            }
        }
    }

    private void handleRenameRequest(Player player) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (Main.pendingRenames.isEmpty()) {
            player.sendMessage("§7No pending rename requests.");
            return;
        }

        player.sendMessage("§6§l--- Pending Rename Requests ---");

        for (Map.Entry<String, String> entry : Main.pendingRenames.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();
            String requester = Main.pendingRenameRequester.getOrDefault(oldName, "Unknown");

            player.sendMessage("§e" + oldName + " §7Ã¢â€ â€™ §f" + newName + " §7(by §f" + requester + "§7)");

            Component approveButton = Component.text("[Approve]")
                    .color(TextColor.color(0x52b788))
                    .decoration(TextDecoration.BOLD, true)
                    .clickEvent(ClickEvent.runCommand("/town approvename " + oldName))
                    .hoverEvent(HoverEvent.showText(Component.text("Approve rename to " + newName)));

            Component denyButton = Component.text("[Deny]")
                    .color(TextColor.color(0xe63946))
                    .decoration(TextDecoration.BOLD, true)
                    .clickEvent(ClickEvent.runCommand("/town denyname " + oldName))
                    .hoverEvent(HoverEvent.showText(Component.text("Deny this rename request")));

            player.sendMessage(
                    approveButton
                            .append(Component.text(" "))
                            .append(denyButton)
            );

            player.sendMessage("§7Approve manually: §f/town approvename " + oldName);
            player.sendMessage("§7Or approve by new name: §f/town approvename " + newName);
            player.sendMessage("§7Deny: §f/town denyname " + oldName);
            player.sendMessage(" ");
        }
    }

    private void handleApproveName(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town approvename <old town name or new town name>");
            return;
        }

        String inputName = getTownNameFromArgs(args, 1);

        String realOldName = null;
        String newName = null;

        for (Map.Entry<String, String> entry : Main.pendingRenames.entrySet()) {
            String oldTownName = entry.getKey();
            String requestedNewName = entry.getValue();

            if (oldTownName.equalsIgnoreCase(inputName) || requestedNewName.equalsIgnoreCase(inputName)) {
                realOldName = oldTownName;
                newName = requestedNewName;
                break;
            }
        }

        if (realOldName == null || newName == null) {
            player.sendMessage("§cNo pending rename request found for that town!");
            return;
        }

        if (findTownName(realOldName) == null) {
            player.sendMessage("§cThe old town does not exist anymore!");

            Main.pendingRenames.remove(realOldName);
            Main.pendingRenameRequester.remove(realOldName);

            saveTownData();
            return;
        }

        if (findTownName(newName) != null) {
            player.sendMessage("§cA town with the new name already exists!");
            return;
        }

        renameTown(realOldName, newName);

        Main.pendingRenames.remove(realOldName);
        Main.pendingRenameRequester.remove(realOldName);

        Main.logTownAction(newName, player.getName() + " approved town rename from " + realOldName + " to " + newName + ".");
        saveTownData();

        player.sendMessage("§aApproved rename of §f" + realOldName + " §ato §f" + newName + "§a!");
        Bukkit.broadcastMessage("§6§l[Town] §r§f" + realOldName + " §7has been renamed to §f" + newName + "§7!");
    }

    private void handleDenyName(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town denyname <old town name or new town name>");
            return;
        }

        String inputName = getTownNameFromArgs(args, 1);

        String realOldName = null;
        String deniedName = null;

        for (Map.Entry<String, String> entry : Main.pendingRenames.entrySet()) {
            String oldTownName = entry.getKey();
            String requestedNewName = entry.getValue();

            if (oldTownName.equalsIgnoreCase(inputName) || requestedNewName.equalsIgnoreCase(inputName)) {
                realOldName = oldTownName;
                deniedName = requestedNewName;
                break;
            }
        }

        if (realOldName == null || deniedName == null) {
            player.sendMessage("§cNo pending rename request found for that town!");
            return;
        }

        Main.pendingRenames.remove(realOldName);
        Main.pendingRenameRequester.remove(realOldName);

        Main.logTownAction(realOldName, player.getName() + " denied rename request to " + deniedName + ".");
        saveTownData();

        player.sendMessage("§cDenied rename request for §f" + realOldName + "§c!");

        UUID ownerUUID = Main.townOwner.get(realOldName);

        if (ownerUUID != null) {
            Player owner = Bukkit.getPlayer(ownerUUID);

            if (owner != null) {
                owner.sendMessage("§cYour rename request to §f" + deniedName + " §cwas denied by staff.");
            }
        }
    }

    private void renameTown(String oldName, String newName) {

        if (Main.townLevel.containsKey(oldName)) Main.townLevel.put(newName, Main.townLevel.remove(oldName));
        if (Main.townBank.containsKey(oldName)) Main.townBank.put(newName, Main.townBank.remove(oldName));
        if (Main.townChunks.containsKey(oldName)) Main.townChunks.put(newName, Main.townChunks.remove(oldName));
        if (Main.townOwner.containsKey(oldName)) Main.townOwner.put(newName, Main.townOwner.remove(oldName));
        if (Main.townAssistant.containsKey(oldName)) Main.townAssistant.put(newName, Main.townAssistant.remove(oldName));
        if (Main.townMembers.containsKey(oldName)) Main.townMembers.put(newName, Main.townMembers.remove(oldName));
        if (Main.townAllies.containsKey(oldName)) Main.townAllies.put(newName, Main.townAllies.remove(oldName));
        if (Main.townWars.containsKey(oldName)) Main.townWars.put(newName, Main.townWars.remove(oldName));
        if (Main.townEnemies.containsKey(oldName)) Main.townEnemies.put(newName, Main.townEnemies.remove(oldName));
        if (Main.pendingAllyRequests.containsKey(oldName)) Main.pendingAllyRequests.put(newName, Main.pendingAllyRequests.remove(oldName));
        if (Main.townSpawn.containsKey(oldName)) Main.townSpawn.put(newName, Main.townSpawn.remove(oldName));
        if (Main.townCoreLocation.containsKey(oldName)) Main.townCoreLocation.put(newName, Main.townCoreLocation.remove(oldName));
        if (Main.townSpawnCooldown.containsKey(oldName)) Main.townSpawnCooldown.put(newName, Main.townSpawnCooldown.remove(oldName));
        if (Main.townMotd.containsKey(oldName)) Main.townMotd.put(newName, Main.townMotd.remove(oldName));
        if (Main.lockedBlocks.containsKey(oldName)) Main.lockedBlocks.put(newName, Main.lockedBlocks.remove(oldName));
        if (Main.townMobsEnabled.containsKey(oldName)) Main.townMobsEnabled.put(newName, Main.townMobsEnabled.remove(oldName));
        if (Main.townAuditLogs.containsKey(oldName)) Main.townAuditLogs.put(newName, Main.townAuditLogs.remove(oldName));
        if (Main.townPublicJoin.containsKey(oldName)) Main.townPublicJoin.put(newName, Main.townPublicJoin.remove(oldName));

        if (Main.townChatFormat.containsKey(oldName)) Main.townChatFormat.put(newName, Main.townChatFormat.remove(oldName));

        if (Main.townRenameUsed.containsKey(oldName)) {
            Main.townRenameUsed.put(newName, true);
            Main.townRenameUsed.remove(oldName);
        }

        for (Map.Entry<UUID, String> entry : Main.playerTown.entrySet()) {
            if (entry.getValue().equals(oldName)) {
                Main.playerTown.put(entry.getKey(), newName);
            }
        }

        for (Set<String> allies : Main.townAllies.values()) {
            if (allies.remove(oldName)) allies.add(newName);
        }

        for (Set<String> enemies : Main.townEnemies.values()) {
            if (enemies.remove(oldName)) enemies.add(newName);
        }

        for (Set<String> wars : Main.townWars.values()) {
            if (wars.remove(oldName)) wars.add(newName);
        }

        for (Set<String> requests : Main.pendingAllyRequests.values()) {
            if (requests.remove(oldName)) requests.add(newName);
        }

        if (TownLevelManager.townTaskXP.containsKey(oldName)) {
            TownLevelManager.townTaskXP.put(newName, TownLevelManager.townTaskXP.remove(oldName));
        }

        if (TownLevelManager.townTaskLevel.containsKey(oldName)) {
            TownLevelManager.townTaskLevel.put(newName, TownLevelManager.townTaskLevel.remove(oldName));
        }

        Main.townRenameUsed.put(newName, true);

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        StorageManager.renameTownFolder(plugin, oldName, newName);

    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town invite <player>");
            return;
        }

        String townName = getTown(player);

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can invite players!");
            return;
        }

        if (InviteManager.isOnInviteCooldown(player)) {
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage("§cPlayer not found or not online!");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (Main.playerTown.containsKey(targetUUID)) {
            player.sendMessage("§cThat player already has a town!");
            return;
        }

        InviteManager.addInvite(targetUUID, townName, player.getName());

        Main.logTownAction(townName, player.getName() + " invited " + target.getName() + " to the town.");
        saveTownData();

        target.sendMessage("§6§l[Town Invite] §r§e" + player.getName()
                + " §7has invited you to join §f" + townName + "§7!");
        target.sendMessage("§aType §f/town accept §aor §c/town deny §ato respond.");

        player.sendMessage("§aInvite sent to §f" + target.getName() + "§a!");
    }

    private void handleAdminClaim(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town adminclaim <town>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Main.getChunkKey(chunk);
        String currentOwner = getTownAtChunk(chunkKey);

        if (currentOwner != null && !currentOwner.equalsIgnoreCase(townName)) {
            Main.townChunks.getOrDefault(currentOwner, new HashSet<>()).remove(chunkKey);
            Main.logTownAction(currentOwner, "Staff removed claim " + chunkKey + " and transferred it to " + townName + ".");
        }

        Main.townChunks.putIfAbsent(townName, new HashSet<>());
        Main.townChunks.get(townName).add(chunkKey);

        Main.logTownAction(townName, "Staff " + player.getName() + " force-claimed chunk " + chunkKey + ".");
        saveTownData();

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownCommand.class);
        TownVisualManager.showClaimBorder(player, chunk, plugin);

        player.sendMessage("§aForce-claimed chunk §f" + chunkKey + " §afor §f" + townName + "§a.");
    }

    private void handleAdminDisband(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town admindisband <town>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        disbandTown(townName, player.getName(), true);
        player.sendMessage("§aForce-disbanded §f" + townName + "§a.");
    }

    private void handleAdminSetLevel(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /town adminsetlevel <town> <level>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1, args.length - 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        int level;

        try {
            level = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException exception) {
            player.sendMessage("§cInvalid level!");
            return;
        }

        if (level < 1) {
            player.sendMessage("§cLevel must be at least 1.");
            return;
        }

        Main.townLevel.put(townName, level);

        Main.logTownAction(townName, "Staff " + player.getName() + " set town level to " + level + ".");
        saveTownData();

        player.sendMessage("§aSet §f" + townName + "§a's level to §f" + level + "§a.");
    }

    private void handleAdminBank(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /town adminbank <town> <amount>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1, args.length - 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(args[args.length - 1]);
        } catch (NumberFormatException exception) {
            player.sendMessage("§cInvalid amount!");
            return;
        }

        if (amount < 0) {
            player.sendMessage("§cAmount cannot be negative.");
            return;
        }

        Main.townBank.put(townName, amount);

        Main.logTownAction(townName, "Staff " + player.getName() + " set town bank to $" + String.format("%.2f", amount) + ".");
        saveTownData();

        player.sendMessage("§aSet §f" + townName + "§a's bank to §f$" + String.format("%.2f", amount) + "§a.");
    }

    private void handleRollback(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town rollback <town> [minutes]");
            player.sendMessage("§7Rolls back all block changes in the specified time period.");
            player.sendMessage("§7Default: §f60 §7minutes. Max: §f1440 §7minutes (24 hours).");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        int minutes = 60;

        if (args.length >= 3) {
            try {
                minutes = Integer.parseInt(args[2]);
                if (minutes < 1) {
                    player.sendMessage("§cMinutes must be at least 1. Using 1 minute.");
                    minutes = 1;
                }
                if (minutes > 1440) {
                    player.sendMessage("§cMinutes cannot exceed 1440 (24 hours). Using 1440 minutes.");
                    minutes = 1440;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid minutes! Using default 60 minutes.");
                minutes = 60;
            }
        }

        if (TownRollbackManager.isRollbackInProgress()) {
            player.sendMessage("§cA rollback is already in progress! Please wait.");
            return;
        }

        player.sendMessage("§eRolling back §f" + townName + " §echanges from the last §f" + minutes + " §eminutes...");

        int rolledBack = TownRollbackManager.rollbackTown(townName, minutes);

        if (rolledBack == 0) {
            player.sendMessage("§cNo rollback data found for §f" + townName + " §cin the last §f" + minutes + " §cminutes.");
            player.sendMessage("§7Block changes are only recorded for 24 hours.");
            return;
        }

        Main.logTownAction(townName, "Staff " + player.getName() + " rolled back " + rolledBack + " block changes.");
        saveTownData();

        Bukkit.broadcastMessage("§d§l[Staff] §r§f" + player.getName()
                + " §7rolled back §f" + rolledBack + " §7block changes in §f" + townName + "§7.");

        player.sendMessage("§aSuccessfully rolled back §f" + rolledBack + " §ablock changes in §f" + townName + "§a.");
    }

    private void handleRollbackInfo(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town rollbackinfo <town>");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        int townHistorySize = TownRollbackManager.getTownHistorySize(townName);
        int totalHistorySize = TownRollbackManager.getHistorySize();

        player.sendMessage("§6§l--- Rollback Info for " + townName + " ---");
        player.sendMessage("§eSaved rollback entries: §f" + townHistorySize);
        player.sendMessage("§eTotal entries (all towns): §f" + totalHistorySize);
        player.sendMessage("§7Entries are automatically deleted after §f24 hours§7.");

        if (townHistorySize == 0) {
            player.sendMessage("§7No rollback data available for this town.");
            player.sendMessage("§7Block changes are only recorded when players break/place blocks.");
        } else {
            player.sendMessage("§7Use §f/town rollback " + townName + " §7to roll back changes.");
            player.sendMessage("§7Example: §f/town rollback " + townName + " 30 §7(rolls back last 30 minutes)");
        }
    }

    private void handleAdminWars(Player player) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        List<WarManager.War> wars = WarManager.getAllWars();

        player.sendMessage("§4§l--- Active Wars ---");
        player.sendMessage("§eAuto Sessions: " + (WarManager.isAutomaticWarSessionsEnabled() ? "§aEnabled" : "§cDisabled"));

        if (wars.isEmpty()) {
            player.sendMessage("§7No wars are currently declared.");
            return;
        }

        for (WarManager.War war : wars) {
            String warId = WarManager.getWarId(war);

            Component line = Component.text("Ã¢â‚¬Â¢ ")
                    .color(TextColor.color(0x777777))
                    .append(Component.text(warId)
                            .color(TextColor.color(0xF4A261))
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" | ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(war.attackerTown)
                            .color(TextColor.color(0xE63946)))
                    .append(Component.text(" vs ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(war.defenderTown)
                            .color(TextColor.color(0x52B788)))
                    .append(Component.text(" | Score: ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(war.attackerPoints + "-" + war.defenderPoints)
                            .color(TextColor.color(0xFFFFFF)))
                    .append(Component.text(" | Session: ")
                            .color(TextColor.color(0xAAAAAA)))
                    .append(Component.text(war.activeSession ? "Active" : "Inactive")
                            .color(war.activeSession ? TextColor.color(0xE63946) : TextColor.color(0xAAAAAA)))
                    .hoverEvent(HoverEvent.showText(Component.text(
                            "War ID: " + warId
                                    + "\nAttacker: " + war.attackerTown
                                    + "\nDefender: " + war.defenderTown
                                    + "\nScore: " + war.attackerPoints + "-" + war.defenderPoints
                                    + "\nActive Session: " + war.activeSession
                                    + "\nCore Health: " + WarManager.getCoreHealthPercent(war.defenderTown) + "%"
                    )));

            player.sendMessage(line);

            if (war.activeSession) {
                player.sendMessage("§7  End session: §f/town adminendwar " + warId);
            } else {
                player.sendMessage("§7  Start session: §f/town adminstartwar " + war.attackerTown + " 120");
            }

            player.sendMessage("§7  Terminate war: §f/town warterminate " + warId);
        }
    }

    private void handleAdminStartWar(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /town adminstartwar <town> <minutes>");
            player.sendMessage("§7Example: §f/town adminstartwar MyTown 120");
            return;
        }

        String inputTown = getTownNameFromArgs(args, 1, args.length - 1);
        String townName = findTownName(inputTown);

        if (townName == null) {
            player.sendMessage("§cThat town does not exist!");
            return;
        }

        int minutes;

        try {
            minutes = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException exception) {
            player.sendMessage("§cInvalid minutes!");
            return;
        }

        if (minutes < 1) {
            player.sendMessage("§cMinutes must be at least 1.");
            return;
        }

        if (minutes > 180) {
            player.sendMessage("§cMinutes cannot be higher than 180.");
            return;
        }

        WarManager.War war = WarManager.getWarByTown(townName);

        if (war == null) {
            player.sendMessage("§cThat town is not in a war!");
            return;
        }

        if (war.activeSession) {
            player.sendMessage("§cThat war session is already active!");
            return;
        }

        if (WarManager.hasActiveWarSession()) {
            player.sendMessage("§cAnother war session is already active!");
            player.sendMessage("§7Only one war session can run at a time.");
            return;
        }

        boolean started = WarManager.forceStartSession(townName, minutes, player.getName());

        if (!started) {
            player.sendMessage("§cFailed to start war session.");
            return;
        }

        saveTownData();

        player.sendMessage("§aStarted war session §f" + WarManager.getWarId(war)
                + " §afor §f" + minutes + " minutes§a.");
    }

    private void handleAdminEndWar(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town adminendwar <warId>");
            player.sendMessage("§7Use §f/town adminwars §7to see war IDs.");
            return;
        }

        String warId = args[1];
        WarManager.War war = WarManager.getWarById(warId);

        if (war == null) {
            player.sendMessage("§cNo war found with that ID.");
            player.sendMessage("§7Use §f/town adminwars §7to see war IDs.");
            return;
        }

        if (!war.activeSession) {
            player.sendMessage("§cThat war does not have an active session.");
            return;
        }

        boolean ended = WarManager.forceEndSessionById(warId, player.getName());

        if (!ended) {
            player.sendMessage("§cFailed to end war session.");
            return;
        }

        saveTownData();

        player.sendMessage("§aEnded war session §f" + warId + "§a.");
    }

    private void handleWarTerminate(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /town warterminate <warId>");
            player.sendMessage("§7This completely deletes the war for both sides.");
            player.sendMessage("§7Use §f/town adminwars §7to see war IDs.");
            return;
        }

        String warId = args[1];
        WarManager.War war = WarManager.getWarById(warId);

        if (war == null) {
            player.sendMessage("§cNo war found with that ID.");
            player.sendMessage("§7Use §f/town adminwars §7to see war IDs.");
            return;
        }

        String attackerTown = war.attackerTown;
        String defenderTown = war.defenderTown;

        WarManager.removeWar(war);


        Main.logTownAction(attackerTown, "Staff " + player.getName() + " terminated the war against " + defenderTown + ".");
        Main.logTownAction(defenderTown, "Staff " + player.getName() + " terminated the war against " + attackerTown + ".");

        saveTownData();

        Bukkit.broadcastMessage("§4§l[WAR] §r§cStaff has terminated the war between §f"
                + attackerTown + " §cand §f" + defenderTown + "§c.");

        player.sendMessage("§aTerminated war §f" + warId + "§a.");
    }

    private void handleAdminWarAuto(Player player, String[] args) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            player.sendMessage("§4§l--- Automatic War Sessions ---");
            player.sendMessage("§eStatus: " + (WarManager.isAutomaticWarSessionsEnabled() ? "§aEnabled" : "§cDisabled"));
            player.sendMessage("§7Use §f/town adminwarauto on §7or §f/town adminwarauto off§7.");
            return;
        }

        if (args[1].equalsIgnoreCase("on")) {
            WarManager.setAutomaticWarSessionsEnabled(true);
            player.sendMessage("§aAutomatic war sessions enabled.");
            Bukkit.broadcastMessage("§4§l[WAR] §r§aAutomatic war sessions have been enabled by staff.");
            saveTownData();
            return;
        }

        if (args[1].equalsIgnoreCase("off")) {
            WarManager.setAutomaticWarSessionsEnabled(false);
            player.sendMessage("§cAutomatic war sessions disabled.");
            Bukkit.broadcastMessage("§4§l[WAR] §r§cAutomatic war sessions have been disabled by staff.");
            saveTownData();
            return;
        }

        player.sendMessage("§cUsage: /town adminwarauto on/off/status");
    }

    private String getTownNameFromArgs(String[] args, int startIndex, int endExclusive) {
        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < endExclusive; i++) {
            if (i > startIndex) builder.append(" ");
            builder.append(args[i]);
        }

        return builder.toString();
    }

    private String getRelationDisplay(String playerTown, String ownerTown) {
        if (playerTown.equalsIgnoreCase(ownerTown)) {
            return "§aHome";
        }

        if (Main.townWars.getOrDefault(playerTown, Collections.emptySet()).contains(ownerTown)) {
            return "§4At War";
        }

        if (Main.townAllies.getOrDefault(playerTown, Collections.emptySet()).contains(ownerTown)) {
            return "§bAlly";
        }

        if (Main.townEnemies.getOrDefault(playerTown, Collections.emptySet()).contains(ownerTown)) {
            return "§cEnemy";
        }

        if (Main.townEnemies.getOrDefault(ownerTown, Collections.emptySet()).contains(playerTown)) {
            return "§6Hostile";
        }

        return "§eNeutral";
    }

    public static String getMobStatusLine(String townName) {
        int level = Main.townLevel.getOrDefault(townName, 1);

        if (level < 5) {
            return "§eEnabled §7(Level 5 toggle locked)";
        }

        boolean enabled = Main.townMobsEnabled.getOrDefault(townName, true);
        return enabled ? "§aEnabled" : "§cDisabled";
    }

    public static String stripColorCodes(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private String formatTownSet(Set<String> towns) {
        if (towns == null || towns.isEmpty()) {
            return "None";
        }

        return String.join(", ", towns);
    }

    public static String getRulerName(String townName) {
        UUID ownerUUID = Main.townOwner.get(townName);

        if (ownerUUID == null) {
            return "Unknown";
        }

        String name = Bukkit.getOfflinePlayer(ownerUUID).getName();
        return name != null ? name : "Unknown";
    }

    public static String getAssistantName(String townName) {
        UUID assistantUUID = Main.townAssistant.get(townName);

        if (assistantUUID == null) {
            return "None";
        }

        String name = Bukkit.getOfflinePlayer(assistantUUID).getName();
        return name != null ? name : "Unknown";
    }

    private UUID findTownMemberUUID(String townName, String inputName) {
        if (townName == null || inputName == null) {
            return null;
        }

        Player onlineTarget = Bukkit.getPlayerExact(inputName);

        if (onlineTarget != null) {
            UUID uuid = onlineTarget.getUniqueId();

            if (townName.equals(Main.playerTown.get(uuid))) {
                return uuid;
            }
        }

        for (UUID uuid : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            String name = getOfflineName(uuid);

            if (name.equalsIgnoreCase(inputName)) {
                return uuid;
            }
        }

        return null;
    }

    private String getOfflineName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }

        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Unknown";
    }

    private Component buildMemberHoverLine(UUID uuid, String displayName) {
        String role = Main.playerRole.getOrDefault(uuid, "member");
        String title = Main.playerTownTitle.getOrDefault(uuid, "");
        String shownTitle = title == null || title.isBlank()
                ? "No Title"
                : ChatListener.translateColors(title);

        String lastOnline = Main.formatLastOnline(uuid);

        Component hover = Component.text()
                .append(Component.text("Title: ").color(TextColor.color(0xAAAAAA)))
                .append(Component.text(shownTitle).color(TextColor.color(0xFFFFFF)))
                .append(Component.text("\nRole: ").color(TextColor.color(0xAAAAAA)))
                .append(Component.text(capitalize(role)).color(TextColor.color(0xFFFFFF)))
                .append(Component.text("\nLast Online: ").color(TextColor.color(0xAAAAAA)))
                .append(Component.text(lastOnline).color(TextColor.color(0xFFFFFF)))
                .build();

        String titlePrefix = title == null || title.isBlank()
                ? ""
                : "§7[" + ChatListener.translateColors(title) + "§7] ";

        return Component.text(titlePrefix + "§f" + displayName)
                .hoverEvent(HoverEvent.showText(hover));
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Unknown";
        }

        return location.getWorld().getName()
                + " X:" + location.getBlockX()
                + " Y:" + location.getBlockY()
                + " Z:" + location.getBlockZ();
    }

    private String formatUUID(UUID uuid) {
        return uuid == null ? "none" : uuid.toString();
    }

    private void playUnclaimSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
        } catch (NoSuchFieldError error) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);
        }
    }

    private int getRequiredXPForTask(String task) {
        return TownLevelManager.getRequiredXPForTask(task, 1);
    }

    private String formatTaskName(String task) {
        if (task == null || task.isEmpty()) return "Unknown";

        return switch (task.toLowerCase()) {
            case "pve" -> "PvE";
            case "pvp" -> "PvP";
            default -> {
                String[] words = task.replace("_", " ").split(" ");
                StringBuilder formatted = new StringBuilder();

                for (String word : words) {
                    if (word.isEmpty()) continue;

                    if (!formatted.isEmpty()) {
                        formatted.append(" ");
                    }

                    formatted.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                }

                yield formatted.toString();
            }
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void handlePlot(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§6§l--- Town Plots ---");
            player.sendMessage("§f/town plot subdivide <4|16> §7- Divide chunk into plots");
            player.sendMessage("§f/town plot setprice <amount> §7- Set price for current plot");
            player.sendMessage("§f/town plot buy §7- Buy the current plot");
            player.sendMessage("§f/town plot unclaim §7- Evict plot owner (Ruler only)");
            player.sendMessage("§f/town plot clear §7- Remove subdivision from chunk");
            player.sendMessage("§f/town plot info §7- View plot details");
            return;
        }

        String sub = args[1].toLowerCase();
        String townName = getTown(player);
        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Main.getChunkKey(chunk);

        switch (sub) {
            case "subdivide" -> handlePlotSubdivide(player, townName, chunkKey, args);
            case "setprice" -> handlePlotSetPrice(player, townName, chunkKey, args);
            case "buy" -> handlePlotBuy(player, townName, chunkKey);
            case "info" -> handlePlotInfo(player, chunkKey);
            case "unclaim" -> handlePlotUnclaim(player, townName, chunkKey);
            case "clear" -> handlePlotClear(player, townName, chunkKey);
            default -> player.sendMessage("§cUnknown plot command. Use /town plot for help.");
        }
    }

    private void handlePlotSubdivide(Player player, String townName, String chunkKey, String[] args) {
        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can subdivide chunks!");
            return;
        }

        if (!Main.townChunks.getOrDefault(townName, Collections.emptySet()).contains(chunkKey)) {
            player.sendMessage("§cThis chunk is not claimed by your town!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /town plot subdivide <4|16>");
            return;
        }

        int type;
        try {
            type = Integer.parseInt(args[2]);
            if (type != 4 && type != 16) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid type. Choose 4 or 16 plots.");
            return;
        }

        Main.subdividedChunks.put(chunkKey, type);
        Main.logTownAction(townName, player.getName() + " subdivided chunk " + chunkKey + " into " + type + " plots.");
        saveTownData();
        player.sendMessage("§aSuccessfully subdivided this chunk into §f" + type + " §aplots!");
    }

    private void handlePlotSetPrice(Player player, String townName, String chunkKey, String[] args) {
        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can set plot prices!");
            return;
        }

        if (!Main.subdividedChunks.containsKey(chunkKey)) {
            player.sendMessage("§cThis chunk is not subdivided into plots!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /town plot setprice <amount>");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price amount.");
            return;
        }

        int type = Main.subdividedChunks.get(chunkKey);
        int index = Main.getPlotIndex(player.getLocation(), type);
        String plotKey = chunkKey + ":" + index;

        Main.Plot plot = Main.townPlots.getOrDefault(plotKey, new Main.Plot(null, 0));
        plot.price = price;
        Main.townPlots.put(plotKey, plot);

        saveTownData();
        player.sendMessage("§aPrice for this plot set to §f$" + String.format("%.2f", price) + "§a.");
    }

    private void handlePlotBuy(Player player, String townName, String chunkKey) {
        if (!Main.subdividedChunks.containsKey(chunkKey)) {
            player.sendMessage("§cThis chunk is not subdivided into plots!");
            return;
        }

        int type = Main.subdividedChunks.get(chunkKey);
        int index = Main.getPlotIndex(player.getLocation(), type);
        String plotKey = chunkKey + ":" + index;

        Main.Plot plot = Main.townPlots.get(plotKey);
        if (plot == null) {
            plot = new Main.Plot(null, 0);
            Main.townPlots.put(plotKey, plot);
        }

        if (plot.owner != null) {
            player.sendMessage("§cThis plot is already owned by §f" + Main.getPlotOwnerName(plot) + "§c.");
            return;
        }

        double price = plot.price;
        if (price > 0 && Main.economy != null) {
            if (!Main.economy.has(player, price)) {
                player.sendMessage("§cYou don't have enough money to buy this plot! Cost: §f$" + String.format("%.2f", price));
                return;
            }

            Main.economy.withdrawPlayer(player, price);
            Main.townBank.put(townName, Main.townBank.getOrDefault(townName, 0.0) + price);
            player.sendMessage("§aYou paid §f$" + String.format("%.2f", price) + " §ato your town bank.");
        }

        plot.owner = player.getUniqueId();
        Main.logTownAction(townName, player.getName() + " purchased plot " + plotKey + ".");
        saveTownData();

        player.sendMessage("§aCongratulations! You now own this plot.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
    }

    private void handlePlotInfo(Player player, String chunkKey) {
        if (!Main.subdividedChunks.containsKey(chunkKey)) {
            player.sendMessage("§cThis chunk is not subdivided into plots.");
            return;
        }

        int type = Main.subdividedChunks.get(chunkKey);
        int index = Main.getPlotIndex(player.getLocation(), type);
        String plotKey = chunkKey + ":" + index;
        Main.Plot plot = Main.townPlots.get(plotKey);

        player.sendMessage("§6§l--- Plot Info ---");
        player.sendMessage("§eSubdivision: §f" + type + " plots");
        player.sendMessage("§ePlot Index: §f#" + index);
        player.sendMessage("§eOwner: §f" + Main.getPlotOwnerName(plot));
        player.sendMessage("§ePrice: §f$" + String.format("%.2f", plot != null ? plot.price : 0));
    }

    private void handlePlotUnclaim(Player player, String townName, String chunkKey) {
        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can evict plot owners!");
            return;
        }

        if (!Main.subdividedChunks.containsKey(chunkKey)) {
            player.sendMessage("§cThis chunk is not subdivided.");
            return;
        }

        int type = Main.subdividedChunks.get(chunkKey);
        int index = Main.getPlotIndex(player.getLocation(), type);
        String plotKey = chunkKey + ":" + index;

        if (Main.townPlots.remove(plotKey) != null) {
            Main.logTownAction(townName, player.getName() + " unmapped plot " + plotKey + ".");
            saveTownData();
            player.sendMessage("§aPlot owner evicted and plot cleared.");
        } else {
            player.sendMessage("§cThis plot was already unowned.");
        }
    }

    private void handlePlotClear(Player player, String townName, String chunkKey) {
        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly ruler or assistant can clear subdivisions!");
            return;
        }

        if (Main.subdividedChunks.remove(chunkKey) != null) {
            // Also remove all plots in this chunk
            Main.townPlots.entrySet().removeIf(e -> e.getKey().startsWith(chunkKey + ":"));
            Main.logTownAction(townName, player.getName() + " removed subdivisions from chunk " + chunkKey + ".");
            saveTownData();
            player.sendMessage("§aChunk subdivision cleared. All plots in this chunk are now public town land.");
        } else {
            player.sendMessage("§cThis chunk was not subdivided.");
        }
    }
}
