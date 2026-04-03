package me.a8kj.world.backup;

public interface BackupTask {
    void execute();
    String getTaskName();
}
