package io.github.silentdevelopment.headdb.paper.item;

import io.github.silentdevelopment.headdb.model.Head;
import org.bukkit.inventory.ItemStack;

public interface HeadItemFactory {

    ItemStack create(Head head);

}