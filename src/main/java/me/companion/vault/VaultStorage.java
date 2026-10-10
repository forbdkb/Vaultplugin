package me.companion.vault;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
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

    /**
     * حفظ الصندوق بطريقة Base64 (الطريقة الصحيحة).
     */
    public void saveVault(UUID owner, ItemStack[] contents) {
        synchronized (lock) {
            if (contents == null) return;
            try {
                String encoded = encodeItems(contents);
                config.set("vaults." + owner.toString() + ".data", encoded);
                config.set("vaults." + owner.toString() + ".lastSave", System.currentTimeMillis());
                atomicSave();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save vault for " + owner + ": " + e.getMessage());
            }
        }
    }

    /**
     * استرجاع الصندوق من Base64.
     */
    public ItemStack[] loadVault(UUID owner) {
        synchronized (lock) {
            try {
                String encoded = config.getString("vaults." + owner.toString() + ".data");
                if (encoded == null || encoded.isEmpty()) {
                    return new ItemStack[54];
                }
                ItemStack[] items = decodeItems(encoded);
                if (items.length == 54) {
                    return items;
                }
                ItemStack[] fixed = new ItemStack[54];
                System.arraycopy(items, 0, fixed, 0, Math.min(items.length, 54));
                return fixed;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load vault for " + owner + ": " + e.getMessage());
                return new ItemStack[54];
            }
        }
    }

    public void deleteVault(UUID owner) {
        synchronized (lock) {
            config.set("vaults." + owner.toString(), null);
            atomicSave();
        }
    }

    public boolean hasVault(UUID owner) {
        synchronized (lock) {
            return config.contains("vaults." + owner.toString() + ".data");
        }
    }

    public void saveAll() {
        synchronized (lock) {
            atomicSave();
        }
    }

    /**
     * حفظ آمن (Atomic) — يمنع تلف الملف.
     */
    private void atomicSave() {
        try {
            File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
            config.save(tempFile);
            java.nio.file.Files.move(
                tempFile.toPath(),
                file.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            // إذا فشل Atomic، نجرب الطريقة العادية
            try {
                config.save(file);
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to save vaults.yml: " + ex.getMessage());
            }
        }
    }

    /**
     * تحويل ItemStack[] إلى نص Base64.
     */
    private String encodeItems(ItemStack[] items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        dataOutput.writeInt(items.length);
        for (ItemStack item : items) {
            dataOutput.writeObject(item);
        }
        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * تحويل نص Base64 إلى ItemStack[].
     */
    private ItemStack[] decodeItems(String data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        int size = dataInput.readInt();
        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            items[i] = (ItemStack) dataInput.readObject();
        }
        dataInput.close();
        return items;
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
                f.delete();
            }
        }
    }
}
