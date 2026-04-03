package me.a8kj.world.backup;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipBackupTask extends BaseBackupTask {

    private final int compressionLevel;
    private final List<String> excludes;

    public ZipBackupTask(JavaPlugin plugin, World world, File backupFolder, ExecutorService executor, int compressionLevel, List<String> excludes) {
        super(plugin, world, backupFolder, executor);
        this.compressionLevel = compressionLevel;
        this.excludes = excludes;
    }

    @Override
    protected void performBackup() throws Exception {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File zipFile = new File(backupFolder, world.getName() + "_" + timestamp + ".zip");

        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            zos.setLevel(compressionLevel);
            Path sourcePath = world.getWorldFolder().toPath();

            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();

                    for (String pattern : excludes) {
                        if (fileName.equalsIgnoreCase(pattern) ||
                                (pattern.startsWith("*") && fileName.endsWith(pattern.substring(1)))) {
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    String entryName = sourcePath.relativize(file).toString().replace(File.separatorChar, '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Override
    public String getTaskName() {
        return "ZIP_" + world.getName();
    }
}