package me.samin.thunderranks;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThunderRanks extends JavaPlugin implements Listener {

    private final Map<String, String> ranks = new HashMap<>();

    private File rankFile;
    private YamlConfiguration rankConfig;

    private final String[] allowedRanks = {
            "Owner",
            "Co-Owner",
            "Admin",
            "Developer",
            "Moderator",
            "PvPer",
            "Builder",
            "Clutcher",
            "Begger",
            "Noob"
    };

    @Override
    public void onEnable() {

        loadRanks();

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("ThunderRanks has been enabled!");
        getLogger().info("Loaded " + ranks.size() + " player ranks.");
    }

    @Override
    public void onDisable() {

        saveRanks();

        getLogger().info("ThunderRanks has been disabled!");
    }

    // =========================
    // COMMAND SYSTEM
    // =========================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("rank")) {
            return false;
        }

        // /rank
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // =========================
        // /rank set
        // =========================

        if (args[0].equalsIgnoreCase("set")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED +
                        "Only players can use this command.");
                return true;
            }

            // /rank set <rank>
            if (args.length == 2) {

                String rank = findRank(args[1]);

                if (rank == null) {
                    player.sendMessage(ChatColor.RED +
                            "Invalid rank!");

                    sendRankList(player);
                    return true;
                }

                // Staff ranks require OP
                if (isStaffRank(rank)
                        && !player.hasPermission("thunderranks.admin")) {

                    player.sendMessage(ChatColor.RED +
                            "You cannot use this rank.");

                    return true;
                }

                setRank(player, rank);

                player.sendMessage(ChatColor.GREEN +
                        "Your rank has been set to "
                        + rank + "!");

                return true;
            }

            // /rank set <player> <rank>
            if (args.length == 3) {

                if (!player.hasPermission("thunderranks.admin")) {

                    player.sendMessage(ChatColor.RED +
                            "You don't have permission to change another player's rank.");

                    return true;
                }

                Player target =
                        getServer().getPlayerExact(args[1]);

                if (target == null) {

                    player.sendMessage(ChatColor.RED +
                            "Player not found.");

                    return true;
                }

                String rank = findRank(args[2]);

                if (rank == null) {

                    player.sendMessage(ChatColor.RED +
                            "Invalid rank!");

                    sendRankList(player);
                    return true;
                }

                setRank(target, rank);

                player.sendMessage(ChatColor.GREEN +
                        target.getName()
                        + "'s rank has been set to "
                        + rank + "!");

                target.sendMessage(ChatColor.GREEN +
                        "Your rank has been set to "
                        + rank + "!");

                return true;
            }

            sendHelp(player);
            return true;
        }

        // =========================
        // /rank reset
        // =========================

        if (args[0].equalsIgnoreCase("reset")) {

            if (!(sender instanceof Player player)) {

                sender.sendMessage(ChatColor.RED +
                        "Only players can use this command.");

                return true;
            }

            // /rank reset
            if (args.length == 1) {

                setRank(player, "Noob");

                player.sendMessage(ChatColor.GREEN +
                        "Your rank has been reset to Noob!");

                return true;
            }

            // /rank reset <player>
            if (args.length == 2) {

                if (!player.hasPermission("thunderranks.admin")) {

                    player.sendMessage(ChatColor.RED +
                            "You don't have permission to reset another player's rank.");

                    return true;
                }

                Player target =
                        getServer().getPlayerExact(args[1]);

                if (target == null) {

                    player.sendMessage(ChatColor.RED +
                            "Player not found.");

                    return true;
                }

                setRank(target, "Noob");

                player.sendMessage(ChatColor.GREEN +
                        target.getName()
                        + "'s rank has been reset.");

                target.sendMessage(ChatColor.GREEN +
                        "Your rank has been reset to Noob!");

                return true;
            }

            sendHelp(player);
            return true;
        }

        // =========================
        // /rank help
        // =========================

        if (args[0].equalsIgnoreCase("help")) {

            sendHelp(sender);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    // =========================
    // CHAT FORMAT
    // =========================

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        String rank = getRank(player);

        event.setFormat(
                ChatColor.GRAY
                        + "["
                        + getRankColor(rank)
                        + rank
                        + ChatColor.GRAY
                        + "] "
                        + ChatColor.WHITE
                        + player.getName()
                        + ChatColor.GRAY
                        + ": "
                        + ChatColor.WHITE
                        + "%2$s"
        );
    }

    // =========================
    // RANK STORAGE
    // =========================

    private void setRank(Player player, String rank) {

        String uuid =
                player.getUniqueId().toString();

        ranks.put(uuid, rank);

        rankConfig.set(
                "players." + uuid,
                rank
        );

        saveRanks();
    }

    private void loadRanks() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        rankFile =
                new File(getDataFolder(), "ranks.yml");

        if (!rankFile.exists()) {

            try {
                rankFile.createNewFile();

            } catch (IOException e) {

                getLogger().severe(
                        "Could not create ranks.yml!"
                );

                e.printStackTrace();
            }
        }

        rankConfig =
                YamlConfiguration.loadConfiguration(rankFile);

        if (rankConfig.contains("players")) {

            for (String uuid :
                    rankConfig.getConfigurationSection(
                            "players"
                    ).getKeys(false)) {

                String rank =
                        rankConfig.getString(
                                "players." + uuid
                        );

                if (rank != null) {
                    ranks.put(uuid, rank);
                }
            }
        }
    }

    private void saveRanks() {

        if (rankConfig == null) {
            return;
        }

        try {

            rankConfig.save(rankFile);

        } catch (IOException e) {

            getLogger().severe(
                    "Could not save ranks.yml!"
            );

            e.printStackTrace();
        }
    }

    // =========================
    // RANK UTILITIES
    // =========================

    private String findRank(String input) {

        for (String rank : allowedRanks) {

            if (rank.equalsIgnoreCase(input)) {
                return rank;
            }
        }

        return null;
    }

    private boolean isStaffRank(String rank) {

        return rank.equalsIgnoreCase("Owner")
                || rank.equalsIgnoreCase("Co-Owner")
                || rank.equalsIgnoreCase("Admin")
                || rank.equalsIgnoreCase("Developer")
                || rank.equalsIgnoreCase("Moderator");
    }

    public String getRank(Player player) {

        return ranks.getOrDefault(
                player.getUniqueId().toString(),
                "Noob"
        );
    }

    private ChatColor getRankColor(String rank) {

        return switch (rank.toLowerCase()) {

            case "owner" ->
                    ChatColor.DARK_RED;

            case "co-owner" ->
                    ChatColor.RED;

            case "admin" ->
                    ChatColor.GOLD;

            case "developer" ->
                    ChatColor.AQUA;

            case "moderator" ->
                    ChatColor.GREEN;

            case "pvper" ->
                    ChatColor.YELLOW;

            case "builder" ->
                    ChatColor.LIGHT_PURPLE;

            case "clutcher" ->
                    ChatColor.BLUE;

            case "begger" ->
                    ChatColor.GRAY;

            default ->
                    ChatColor.WHITE;
        };
    }

    // =========================
    // HELP
    // =========================

    private void sendRankList(CommandSender sender) {

        sender.sendMessage(
                ChatColor.YELLOW
                        + "Available ranks:"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "PvPer, Builder, Clutcher, Begger, Noob"
        );

        if (sender.hasPermission(
                "thunderranks.admin"
        )) {

            sender.sendMessage(
                    ChatColor.RED
                            + "Staff: Owner, Co-Owner, Admin, Developer, Moderator"
            );
        }
    }

    private void sendHelp(CommandSender sender) {

        sender.sendMessage(
                ChatColor.GOLD
                        + "===== ThunderRanks ====="
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/rank set <rank>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/rank reset"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/rank help"
        );

        if (sender.hasPermission(
                "thunderranks.admin"
        )) {

            sender.sendMessage(
                    ChatColor.RED
                            + "/rank set <player> <rank>"
            );

            sender.sendMessage(
                    ChatColor.RED
                            + "/rank reset <player>"
            );
        }
    }
}
