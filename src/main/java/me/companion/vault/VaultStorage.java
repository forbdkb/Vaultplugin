package me.companion.vault;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class VaultStorage {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Object lock = new Object();

    public VaultStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "vaults.yml");
        loadFile();
    }

    private void loadFile() {
        synchronized (lock) {
            if (!file.exists()) {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create vaults.yml: " + e.getMessage());
                }
            }
            this.config = YamlConfiguration.loadConfiguration(file);
        }
    }

    public void saveVault(UUID owner, ItemStack[] contents) {
        synchronized (lock) {
            if (contents == null) return;
            try {
                config.set("vaults." + owner.toString() + ".items", contents);
                config.set("vaults." + owner.toString() + ".lastSave", System.currentTimeMillis());
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save vault for " + owner + ": " + e.getMessage());
            }
        }
    }

    public ItemStack[] loadVault(UUID owner) {
        synchronized (lock) {
            Object raw = config.get("vaults." + owner.toString() + ".items");
            if (raw instanceof ItemStack[]) {
                ItemStack[] items = (ItemStack[]) raw;
                if (items.length == 54) {
                    return items;
                }
                ItemStack[] fixed = new ItemStack[54];
                System.arraycopy(items, 0, fixed, 0, Math.min(items.length, 54));
                return fixed;
            }
            return new ItemStack[54];
        }
    }

    public void deleteVault(UUID owner) {
        synchronized (lock) {
            config.set("vaults." + owner.toString(), null);
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to delete vault: " + e.getMessage());
            }
        }
    }

    public boolean hasVault(UUID owner) {
        synchronized (lock) {
            return config.contains("vaults." + owner.toString());
        }
    }

    public void saveAll() {
        synchronized (lock) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save all vaults: " + e.getMessage());
            }
        }
    }

    public void backup() {
        synchronized (lock) {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File backupFolder = new File(plugin.getDataFolder(), "backups");
                if (!backupFolder.exists()) backupFolder.mkdirs();

                File backupFile = new File(backupFolder, "vaults_" + timestamp + ".yml");
                config.save(backupFile);
                plugin.getLogger().info("Vault backup created: " + backupFile.getName());

                cleanupOldBackups(backupFolder);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            }
        }
    }

    private void cleanupOldBackups(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;
        long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        for (File f : files) {
            if (f.lastModified() < weekAgo) {
                if (f.delete()) {
                    plugin.getLogger().info("Deleted old backup: " + f.getName());
                }
            }
        }
    }
}
