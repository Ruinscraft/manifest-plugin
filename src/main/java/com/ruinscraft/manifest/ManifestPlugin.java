package com.ruinscraft.manifest;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ManifestPlugin extends JavaPlugin implements Listener {

    private Map<String, String> serverModManifest;
    private Map<Player, Map<String, String>> playerMods;
    private Scoreboard scoreboard;
    private Team noModsTeam;

    @Override
    public void onEnable() {
        serverModManifest = new HashMap<>();
        playerMods = new HashMap<>();

        updateServerModManifest();

        getLogger().info("Mods manifest:");
        for (String mod : serverModManifest.keySet()) {
            getLogger().info(mod + " " + serverModManifest.get(mod));
        }
        getLogger().info("We will alert users if they do not have these mods installed and the hashes don't match");

        scoreboard = getServer().getScoreboardManager().getMainScoreboard();
        noModsTeam = scoreboard.registerNewTeam("NOMODS");
        noModsTeam.setPrefix("NO-MODS ");

        getServer().getMessenger().registerIncomingPluginChannel(this, "manifest:mods_manifest", (s, player, bytes) -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
            int size = in.readInt();
            Map<String, String> mods = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                mods.put(in.readUTF(), in.readUTF());
            }
            playerMods.put(player, mods);
        });

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        playerMods.clear();
        noModsTeam.unregister();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                if (!hasMods(player)) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "You are missing mods or have outdated mods which allow you to see the theater screens and more.");
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Learn how to install our mods here: https://ruinscraft.com/servers/steve-cinema/how-to-join/");
                    noModsTeam.addEntry(player.getName());
                }
            }
        }, 60L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerMods.remove(event.getPlayer());
    }

    private void updateServerModManifest() {
        String manifestURL = "https://storage.googleapis.com/stevecinema-us-download/mods/manifest.txt";

        try (Scanner scanner = new Scanner(new URL(manifestURL).openStream())) {
            while (scanner.hasNext()) {
                String fileName = scanner.next();
                String hash = scanner.next();
                serverModManifest.put(fileName, hash);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean hasMods(Player player) {
        if (playerMods.containsKey(player)) {
            Map<String, String> mods = playerMods.get(player);

            for (String serverMod : serverModManifest.keySet()) {
                if (!(mods.containsKey(serverMod)
                        && mods.get(serverMod).equals(serverModManifest.get(serverMod)))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

}
