package me.a8kj.world.backup;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackupPlugin extends JavaPlugin {

    @Getter
    private BackupManager backupManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.backupManager = new BackupManager(this);
        getCommand("backup").setExecutor(new BackupCommand(this, backupManager));

        long intervalTicks = parseIntervalToTicks(getConfig().getString("interval", "1h"));

        getServer().getScheduler().runTaskTimer(this, backupManager::runBackupForAll, intervalTicks, intervalTicks);

        getLogger().info("BackupSystem enabled. Interval: " + getConfig().getString("interval"));
    }

    private long parseIntervalToTicks(String input) {
        long seconds = 3600;
        Pattern p = Pattern.compile("(\\d+)([smh])");
        Matcher m = p.matcher(input.toLowerCase());

        if (m.find()) {
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2);
            switch (unit) {
                case "s": seconds = value; break;
                case "m": seconds = value * 60; break;
                case "h": seconds = value * 3600; break;
            }
        } else {
            try {
                seconds = Long.parseLong(input);
            } catch (NumberFormatException ignored) {}
        }
        return seconds * 20L;
    }

    @Override
    public void onDisable() {
        if (backupManager != null) {
            backupManager.shutdown();
        }
    }
}