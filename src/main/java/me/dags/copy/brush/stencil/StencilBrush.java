package me.dags.copy.brush.stencil;

import com.flowpowered.math.vector.Vector3i;
import me.dags.commandbus.fmt.Fmt;
import me.dags.copy.CopyPasta;
import me.dags.copy.brush.Action;
import me.dags.copy.brush.Aliases;
import me.dags.copy.brush.clipboard.Clipboard;
import me.dags.copy.brush.clipboard.ClipboardBrush;
import me.dags.copy.brush.option.Checks;
import me.dags.copy.brush.option.Option;
import me.dags.copy.brush.option.value.Palette;
import me.dags.copy.brush.option.value.Stencil;
import me.dags.copy.brush.option.value.Translation;
import me.dags.copy.registry.brush.BrushSupplier;
import me.dags.copy.util.fmt;
import org.spongepowered.api.entity.living.player.Player;

/**
 * @author dags <dags@dags.me>
 */
@Aliases({"stencil"})
public class StencilBrush extends ClipboardBrush {

    public static final Option<Stencil> STENCIL = Option.of("stencil", Stencil.EMPTY);
    public static final Option<Integer> DEPTH = Option.of("depth", 1, Checks.range(1, 16));
    public static final Option<Palette> PALETTE = Palette.OPTION;

    public StencilBrush() {
        setOption(PASTE_AIR, false);
        setOption(TRANSLATE, Translation.OVERLAY);
    }

    @Override
    public void primary(Player player, Vector3i pos, Action action) {
        if (action == Action.SECONDARY) {
            setClipboard(Clipboard.empty());
            fmt.info("Cleared stencil").tell(player);
        } else {
            undo(player, getHistory());
        }
    }

    @Override
    public void secondary(Player player, Vector3i pos, Action action) {
        Stencil stencil = getOption(STENCIL);
        Palette palette = getOption(PALETTE);
        int depth = getOption(DEPTH);

        if (palette.isEmpty()) {
            Fmt.warn("Your palette is empty! Use ").stress("/palette <blockstate>").tell(player);
            return;
        }

        if (!stencil.isPresent()) {
            return;
        }

        StencilVolume volume = new StencilVolume(stencil, palette, depth);
        Clipboard clipboard = Clipboard.stencil(player, volume, stencil.getOffset());
        setClipboard(clipboard);
        apply(player, pos, getHistory());
        fmt.sub("Pasting...").tell(CopyPasta.NOTICE_TYPE, player);
    }

    public static BrushSupplier supplier() {
        return player -> new StencilBrush();
    }
}
