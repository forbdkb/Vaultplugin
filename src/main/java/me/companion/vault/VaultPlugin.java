package me.companion.vault;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultPlugin extends JavaPlugin {

    private static VaultPlugin instance;
    private VaultStorage vaultStorage;

    @Override
    public void onEnable() {
        instance = this;

        vaultStorage = new VaultStorage(this);

        getServer().getPluginManager().registerEvents(new VaultListener(this, vaultStorage), this);

        if (getCommand("pv") != null) {
            getCommand("pv").setExecutor(new PlotVaultCommand(vaultStorage));
        } else {
            getLogger().warning("Command 'pv' not found in plugin.yml!");
        }

        // 🔄 حفظ دوري كل 60 ثانية (بدل 30) للأصناف المفتوحة فقط
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Inventory top = p.getOpenInventory().getTopInventory();
                InventoryHolder holder = top.getHolder();
                if (holder instanceof PlotVaultHolder) {
                    PlotVaultHolder vh = (PlotVaultHolder) holder;
                    vaultStorage.saveVault(vh.getOwnerUuid(), top.getContents());
                }
            }
        }, 20L * 60, 20L * 60);

        // 💾 نسخ احتياطي كل ساعة (async — ما يأثر على السيرفر)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            vaultStorage.backup();
        }, 20L * 60 * 60, 20L * 60 * 60);

        getLogger().info("VaultPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (vaultStorage != null) {
            // 🛡️ حفظ كل الصناديق المفتوحة
            for (Player p : Bukkit.getOnlinePlayers()) {
                Inventory top = p.getOpenInventory().getTopInventory();
                InventoryHolder holder = top.getHolder();
                if (holder instanceof PlotVaultHolder) {
                    PlotVaultHolder vh = (PlotVaultHolder) holder;
                    vaultStorage.saveVault(vh.getOwnerUuid(), top.getContents());
                }
            }
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
