package me.a8kj.world.backup;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BackupManager {

    private final JavaPlugin plugin;
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor();
    private final File baseBackupDir;

    public BackupManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.baseBackupDir = new File(plugin.getDataFolder(), "backups");
    }

    public void runBackupForAll() {
        boolean allWorlds = plugin.getConfig().getBoolean("backup-all-worlds", true);

        if (allWorlds) {
            for (World world : Bukkit.getWorlds()) {
                submitTask(world);
            }
        } else {
            List<String> targetWorlds = plugin.getConfig().getStringList("worlds");
            for (String worldName : targetWorlds) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    submitTask(world);
                } else if (plugin.getConfig().getBoolean("verbose-logging")) {
                    plugin.getLogger().warning("World not found: " + worldName);
                }
            }
        }
    }

    public void submitTask(World world) {
        File worldBackupDir = new File(baseBackupDir, world.getName());
        int compression = plugin.getConfig().getInt("compression-level", 5);
        List<String> excludes = plugin.getConfig().getStringList("exclude");

        new ZipBackupTask(plugin, world, worldBackupDir, backupExecutor, compression, excludes) {
            @Override
            protected void postBackupSync() {
                pruneOldBackups(worldBackupDir);

                if (plugin.getConfig().getBoolean("notify-admins-on-completion", true)) {
                    String message = "§6§lBackup §8» §aSuccessfully backed up world: §f" + world.getName();

                    Bukkit.getOnlinePlayers().stream()
                            .filter(player -> player.isOp() || player.hasPermission("backup.admin"))
                            .forEach(player -> player.sendMessage(message));

                    if (plugin.getConfig().getBoolean("verbose-logging", true)) {
                        plugin.getLogger().info("Notification sent to online admins for world: " + world.getName());
                    }
                }
            }
        }.execute();
    }

    private void pruneOldBackups(File folder) {
        int max = plugin.getConfig().getInt("max-backups-per-world", 10);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));

        if (files != null && files.length > max) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - max; i++) {
                files[i].delete();
            }
        }
    }

    public void shutdown() {
        backupExecutor.shutdown();
        try {
            if (!backupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                backupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}