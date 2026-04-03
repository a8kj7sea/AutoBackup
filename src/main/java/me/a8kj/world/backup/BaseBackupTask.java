package me.a8kj.world.backup;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

@RequiredArgsConstructor
public abstract class BaseBackupTask implements BackupTask {

    protected final JavaPlugin plugin;
    protected final World world;
    protected final File backupFolder;
    private final ExecutorService executor;

    @Override
    public final void execute() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::execute);
            return;
        }

        plugin.getLogger().info("[" + world.getName() + "] Flushing chunks to disk...");
        world.save();

        final boolean originalAutoSave = world.isAutoSave();
        world.setAutoSave(false);

        executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                performBackup();
                long duration = System.currentTimeMillis() - startTime;
                plugin.getLogger().info("[" + world.getName() + "] Backup completed in " + duration + "ms");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Backup failed for " + world.getName(), e);
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    world.setAutoSave(originalAutoSave);
                    postBackupSync();
                });
            }
        });
    }

    protected abstract void performBackup() throws Exception;

    protected void postBackupSync() {}
}