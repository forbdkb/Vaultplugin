package me.companion.vault;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultPlugin extends JavaPlugin {

    private static VaultPlugin instance;
    private VaultStorage vaultStorage;

    @Override
    public void onEnable() {
        instance = this;

        // تهيئة التخزين
        vaultStorage = new VaultStorage(this);

        // تسجيل الأحداث
        getServer().getPluginManager().registerEvents(new VaultListener(this, vaultStorage), this);

        // تسجيل الأمر
        if (getCommand("pv") != null) {
            getCommand("pv").setExecutor(new PlotVaultCommand(vaultStorage));
        } else {
            getLogger().warning("Command 'pv' not found in plugin.yml!");
        }

        // جدولة النسخ الاحتياطي كل ساعة
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            vaultStorage.backup();
        }, 20L * 60 * 60, 20L * 60 * 60);

        getLogger().info("VaultPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (vaultStorage != null) {
            vaultStorage.saveAll();
            getLogger().info("All vaults saved.");
        }
        getLogger().info("VaultPlugin disabled.");
    }

    public static VaultPlugin getInstance() {
        return instance;
    }

    public VaultStorage getVaultStorage() {
        return vaultStorage;
    }
}
