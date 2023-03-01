package me.badbones69.crazyauctions.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.logging.Logger;

public class ItemUtil {
    public static void log(@NotNull Logger logger, @NotNull Collection<ItemStack> items) {
        for (ItemStack item : items) {
            log(logger, item);
        }
    }

    public static void log(@NotNull Logger logger, @Nullable ItemStack item) {
        if (item == null) {
            logger.info("  null");
        }
        try {
            logger.info("  " + com.github.mori01231.lifecore.util.ItemUtil.toString(item));
        } catch (NoClassDefFoundError e) {
            logger.info("  " + item);
        }
    }
}
