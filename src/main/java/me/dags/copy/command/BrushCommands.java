package me.dags.copy.command;

import me.dags.commandbus.annotation.*;
import me.dags.commandbus.command.Flags;
import me.dags.commandbus.fmt.PagFormatter;
import me.dags.copy.CopyPasta;
import me.dags.copy.PlayerData;
import me.dags.copy.PlayerManager;
import me.dags.copy.brush.Brush;
import me.dags.copy.brush.option.Option;
import me.dags.copy.brush.option.Value;
import me.dags.copy.brush.stencil.Stencil;
import me.dags.copy.brush.stencil.StencilBrush;
import me.dags.copy.registry.brush.BrushRegistry;
import me.dags.copy.registry.brush.BrushType;
import me.dags.copy.util.fmt;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author dags <dags@dags.me>
 */
public class BrushCommands {

    static  <T extends Brush> Optional<T> getBrush(Player player, Class<T> type) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            if (type.isInstance(brush.get())) {
                return brush.map(type::cast);
            }
            fmt.error("Current brush is not a %s", type.getSimpleName());
        }
        return Optional.empty();
    }

    static Optional<Brush> getBrush(Player player) {
        Optional<ItemType> item = player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem);
        if (!item.isPresent()) {
            fmt.error("You are not holding an item").tell(player);
            return Optional.empty();
        }

        Optional<Brush> brush = PlayerManager.getInstance().get(player).flatMap(data -> data.getBrush(player));
        if (!brush.isPresent()) {
            fmt.error("You have not bound a brush to item %s", item.get().getName()).tell(player);
            return Optional.empty();
        }

        return brush;
    }

    @Permission
    @Command("copy <brush>")
    @Description("Bind the <brush> to your held item")
    public void brush(@Src Player player, BrushType type) {
        ItemType item = player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem).orElse(ItemTypes.NONE);
        PlayerData data = PlayerManager.getInstance().must(player);
        if (item == ItemTypes.NONE) {
            data.removeBrush(item);
            fmt.sub("Removed %s brush", type).tell(player);
        } else {
            Optional<Brush> brush = type.create(player);
            if (brush.isPresent()) {
                data.setBrush(item, brush.get());
                fmt.info("Set brush ").stress(type).info(" to item ").stress(item.getName()).tell(player);
            }
        }
    }

    @Flag("a")
    @Permission
    @Command("copy list")
    @Description("List all brush types")
    public void list(@Src CommandSource source, Flags flags) {
        boolean aliases = flags.has("a");
        PagFormatter page = fmt.page();
        if (aliases) {
            BrushRegistry.getInstance().forEachAlias((s, type) -> page.line().subdued(" - ").stress(s).info(" (%s)", type));
        } else {
            BrushRegistry.getInstance().forEachUnique((s, type) -> page.line().subdued(" - ").stress(s));
        }
        page.sort(true).build().sendTo(source);
    }

    @Permission
    @Command("copy reset")
    @Description("Reset all options for your current brush to their defaults")
    public void reset(@Src Player player) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            fmt.info("Reset brush ").stress(brush.get().getType()).tell(player);
            brush.get().getOptions().reset();
        }
    }

    @Permission
    @Command("copy <option> <value>")
    @Description("Set an option for your current brush")
    public void option(@Src Player player, Option<?> option, Value<?> value) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            BrushType type = brush.get().getType();
            brush.get().setOption(option, value.get());
            fmt.info("Set ").stress(option).info("=").stress(value).info(" for brush ").stress(type).tell(player);
        }
    }

    @Permission
    @Command("copy options")
    @Description("List all options and their values for the current brush")
    public void options(@Src Player player) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            PagFormatter page = fmt.page();
            page.title().stress("%s Options:", brush.get().getType());
            brush.get().getOptions().forEach((option, o) -> page.line().subdued(" - ").stress(option).info("=").stress(o).info(" (%s)", option.getType().getSimpleName()));
            page.sort(true).build().sendTo(player);
        }
    }

    @Permission
    @Command("stencil <url> <samples> <threshold>")
    @Description("Create a line stencil from an image at the given url")
    public void stencil(@Src Player player, String url, int samples, float threshold) {
        Optional<Brush> brush = getBrush(player);
        if (brush.isPresent()) {
            if (brush.get().getType().getType() != StencilBrush.class) {
                fmt.error("You must be using a Stencil Brush").tell(player);
                return;
            }

            Supplier<Optional<Stencil>> supplier = () -> Stencil.fromUrl(url, samples, threshold);
            Consumer<Optional<Stencil>> consumer = stencil -> {
                if (stencil.isPresent()) {
                    brush.get().setOption(StencilBrush.STENCIL, stencil.get());
                    fmt.info("Successfully set your stencil").tell(player);
                } else {
                    fmt.error("Unable to create a stencil from the url %s", url).tell(player);
                }
            };

            fmt.info("Creating stencil...").tell(player);
            CopyPasta.getInstance().submitAsync(supplier, consumer);
        }
    }
}
