package me.companion.vault;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class PlotVaultHolder implements InventoryHolder {

    private final UUID ownerUuid;
    private final UUID viewerUuid;
    private Inventory inventory;

    public PlotVaultHolder(UUID ownerUuid, UUID viewerUuid) {
        this.ownerUuid = ownerUuid;
        this.viewerUuid = viewerUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
