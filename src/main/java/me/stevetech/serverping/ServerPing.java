package me.stevetech.serverping;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;


public class ServerPing extends PlaceholderExpansion implements Taskable {
    private final String offline = "Offline";
    private final String online = "Online";

    private final HashMap<String, MCPingResponse> servers = new HashMap<>();
    private BukkitTask task;
    private boolean taskRunning = false;

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public @NotNull String getAuthor(){
        return "Steve-Tech";
    }

    @Override
    public @NotNull String getIdentifier(){
        return "ServerPing";
    }

    @Override
    public @NotNull String getVersion(){
        return "1.0.0";
    }

    @Override
    public void start() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(getPlaceholderAPI(), () -> {
            // If multiple servers timeout the task will take longer than 20 ticks, this will reduce backlog
            if (!taskRunning) {
                taskRunning = true;
                for (String key : servers.keySet()) {
                    String[] host = key.split(":");
                    MCPingOptions options = MCPingOptions.builder()
                        .hostname(host[0])
                        .port(host.length > 1 ? Integer.parseInt(host[1]) : 25565)
                        .timeout(1000).build();
                    try {
                        MCPingResponse reply = MCPing.getPing(options);
                        servers.put(key, reply);
                    } catch (IOException e) {
                        servers.put(key, null); // Removing from here seems to break things
                    }
                }

                while (servers.values().remove(null)); // Remove the server in case its a typo
                taskRunning = false;
            }
        }, 0L, 20L);
    }

    @Override
    public void stop() {
        task.cancel();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier){
        if (identifier.startsWith("status_")) {
            MCPingResponse data = getData(identifier.substring(7));
            return (data != null) ? online : offline;
        } else if (identifier.startsWith("statusc_")) {
            MCPingResponse data = getData(identifier.substring(8));
            return (data != null) ? ChatColor.GREEN + online : ChatColor.RED + offline;
        } else if (identifier.startsWith("ping_")){
            MCPingResponse data = getData(identifier.substring(5));
            return (data != null) ? Long.toString(data.getPing()) : offline;
        } else if (identifier.startsWith("players_")){
            MCPingResponse data = getData(identifier.substring(8));
            return (data != null) ? Integer.toString(data.getPlayers().getOnline()) : offline;
        } else if (identifier.startsWith("maxplayers_")){
            MCPingResponse data = getData(identifier.substring(11));
            return (data != null) ? Integer.toString(data.getPlayers().getMax()) : offline;
        } else if (identifier.startsWith("motd_")){
            MCPingResponse data = getData(identifier.substring(5));
            return (data != null) ? data.getDescription().getText() : offline;
        } else return null;
    }

    // Adds server to map to get data if it doesn't already contain it
    private MCPingResponse getData(String serverIP) {
        if (!servers.containsKey(serverIP)){
            servers.put(serverIP, null);
            return null;
        } else {
            return servers.get(serverIP);
        }
    }
}
