package me.dags.copy.command;

import me.dags.commandbus.annotation.Command;
import me.dags.commandbus.annotation.Src;
import me.dags.commandbus.fmt.Fmt;
import me.dags.copy.CopyPasta;
import me.dags.copy.PlayerData;
import me.dags.copy.brush.Brush;
import me.dags.copy.registry.brush.BrushType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
public class BrushCommands {

    private Optional<Brush> getBrush(Player player) {
        Optional<ItemType> item = player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem);
        if (!item.isPresent()) {
            Fmt.error("You are not holding an item").tell(player);
            return Optional.empty();
        }

        Optional<Brush> brush = CopyPasta.getInstance().getData(player).flatMap(data -> data.getBrush(item.get()));
        if (!brush.isPresent()) {
            Fmt.error("You have not bound a brush to item %s", item.get().getName()).tell(player);
            return Optional.empty();
        }

        return brush;
    }

    @Command("br <brush>")
    public void setBrush(@Src Player player, BrushType type) {
        ItemType item = player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem).orElse(ItemTypes.NONE);
        PlayerData data = CopyPasta.getInstance().ensureData(player);

        if (item == ItemTypes.NONE) {
            data.removeBrush(type.getType());
            Fmt.info("Removed %s brush", type.getName()).tell(player);
        } else {
            Optional<Brush> brush = type.create();
            if (brush.isPresent()) {
                data.resetBrush(brush.get(), item);
                Fmt.info("Assigned %s brush to %s", type.getName(), item.getName()).tell(player);
            }
        }
    }

    @Command("br reset")
    public void reset(@Src Player player) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            reset(player, brush.get().getType());
        }
    }

    @Command("br reset <brush>")
    public void reset(@Src Player player, BrushType type) {
        Optional<PlayerData> data = CopyPasta.getInstance().getData(player);
        if (data.isPresent()) {
            Optional<Brush> brush = type.create();
            if (brush.isPresent() && data.get().resetBrush(brush.get())) {
                Fmt.info("Reset brush %s", type.getId()).tell(player);
            }
        }
    }

    @Command("br <range>")
    public void setRange(@Src Player player, int range) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            int value = Math.min(50, Math.max(1, range));
            brush.get().setRange(value);
            Fmt.info("Set brush %s range to %s", brush.get().getType().getName(), range).tell(player);
        }
    }
}
