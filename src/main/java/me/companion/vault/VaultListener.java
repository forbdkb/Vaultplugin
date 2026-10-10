package me.companion.vault;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class VaultListener implements Listener {

    private final VaultPlugin plugin;
    private final VaultStorage storage;

    public VaultListener(VaultPlugin plugin, VaultStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    /**
     * حفظ عند إغلاق الصندوق
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PlotVaultHolder)) return;

        PlotVaultHolder vaultHolder = (PlotVaultHolder) holder;
        Inventory inv = event.getInventory();

        storage.saveVault(vaultHolder.getOwnerUuid(), inv.getContents());

        if (event.getPlayer() instanceof Player) {
            event.getPlayer().sendMessage("§a✔ Vault saved.");
        }
    }

    /**
     * حفظ فوري عند كل تعديل + حماية من العبث
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PlotVaultHolder)) return;

        PlotVaultHolder vaultHolder = (PlotVaultHolder) holder;

        // منع أي لاعب من العبث بصندوق لاعب آخر
        if (!vaultHolder.getViewerUuid().equals(event.getWhoClicked().getUniqueId())
                && !event.getWhoClicked().isOp()) {
            event.setCancelled(true);
            return;
        }

        // حفظ بعد نبضة
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            storage.saveVault(vaultHolder.getOwnerUuid(), event.getInventory().getContents());
        }, 1L);
    }

    /**
     * حفظ عند السحب والإفلات
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PlotVaultHolder)) return;

        PlotVaultHolder vaultHolder = (PlotVaultHolder) holder;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            storage.saveVault(vaultHolder.getOwnerUuid(), event.getInventory().getContents());
        }, 1L);
    }

    /**
     * حفظ عند خروج اللاعب
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Inventory top = player.getOpenInventory().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (holder instanceof PlotVaultHolder) {
            PlotVaultHolder vaultHolder = (PlotVaultHolder) holder;
            storage.saveVault(vaultHolder.getOwnerUuid(), top.getContents());
        }
    }
}
