package me.companion.vault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlotVaultCommand implements CommandExecutor {

    private final VaultStorage storage;

    public PlotVaultCommand(VaultStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        // /pv → يفتح صندوق اللاعب نفسه
        if (args.length == 0) {
            openVault(player, player.getUniqueId(), player.getName());
            return true;
        }

        // /pv 1 → نفس الشي (توافق)
        if (args[0].equalsIgnoreCase("1")) {
            openVault(player, player.getUniqueId(), player.getName());
            return true;
        }

        // /pv <اسم> → فقط OP
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to view another player's vault.");
            return true;
        }

        // فحص اللاعب أونلاين
        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        if (onlineTarget != null) {
            openVault(player, onlineTarget.getUniqueId(), onlineTarget.getName());
            return true;
        }

        // فحص اللاعب أوفلاين
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
        if (offlineTarget == null || !offlineTarget.hasPlayedBefore() || offlineTarget.getName() == null) {
            player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
            return true;
        }

        openVault(player, offlineTarget.getUniqueId(), offlineTarget.getName());
        return true;
    }

    private void openVault(Player viewer, UUID ownerUuid, String ownerName) {
        PlotVaultHolder holder = new PlotVaultHolder(ownerUuid, viewer.getUniqueId());

        String title = ChatColor.DARK_PURPLE + "Vault: " + ownerName;
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        // استرجاع العناصر المحفوظة
        ItemStack[] saved = storage.loadVault(ownerUuid);
        if (saved != null) {
            inv.setContents(saved);
        }

        viewer.openInventory(inv);
        viewer.sendMessage(ChatColor.GREEN + "Opened vault for " + ownerName);
    }
}
