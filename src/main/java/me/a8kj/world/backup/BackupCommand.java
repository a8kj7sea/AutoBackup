package me.a8kj.world.backup;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class BackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final BackupManager manager;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("backup.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§aBackup configuration reloaded.");
            return true;
        }

        sender.sendMessage("§6§lBackup §8» §eStarting manual backup of all configured worlds...");
        manager.runBackupForAll();
        return true;
    }
}