package me.dags.copy.brush.schematic;

import com.flowpowered.math.vector.Vector3i;
import me.dags.copy.CopyPasta;
import me.dags.copy.block.property.Facing;
import me.dags.copy.brush.Action;
import me.dags.copy.brush.Aliases;
import me.dags.copy.brush.clipboard.ClipboardBrush;
import me.dags.copy.brush.option.Option;
import me.dags.copy.registry.schematic.CachedSchematic;
import me.dags.copy.util.fmt;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.extent.ArchetypeVolume;
import org.spongepowered.api.world.schematic.BlockPaletteTypes;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
@Aliases({"schematic", "schem"})
public class SchematicBrush extends ClipboardBrush {

    public static final Option<SchematicList> SCHEMATICS = Option.of("schematics", SchematicList.class, SchematicList.supplier());
    public static final Option<Mode> MODE = Option.of("mode", Mode.SAVE);
    public static final Option<String> NAME = Option.of("name", "schem");
    public static final Option<String> DIR = Option.of("dir", "");

    @Override
    public void commitSelection(Player player, Vector3i min, Vector3i max, Vector3i origin, int size) {
        Facing horizontal = Facing.getHorizontal(player);
        Facing vertical = Facing.getVertical(player);

        ArchetypeVolume volume = player.getWorld().createArchetypeVolume(min, max, origin);
        Schematic schematic = Schematic.builder()
                .metaValue(Schematic.METADATA_AUTHOR, player.getName())
                .metaValue(CachedSchematic.FACING_H, horizontal.name())
                .metaValue(CachedSchematic.FACING_V, vertical.name())
                .paletteType(BlockPaletteTypes.LOCAL)
                .volume(volume)
                .build();

        Path output = getFilePath();

        try {
            Files.createDirectories(output.getParent());
            try (OutputStream outputStream = Files.newOutputStream(getFilePath())) {
                DataContainer container = DataTranslators.SCHEMATIC.translate(schematic);
                DataFormats.NBT.writeTo(outputStream, container);
            }

            SchematicList list = getOptions().must(SCHEMATICS);
            list.add(new SchematicEntry(output));

            fmt.info("Saved schematic to %s", output).tell(player);
        } catch (IOException e) {
            e.printStackTrace();
            fmt.warn("An error occurred whilst saving schematic to %s", output).tell(player);
        }
    }

    @Override
    public String getPermission() {
        return "brush.schematic";
    }

    @Override
    public void secondary(Player player, Vector3i pos, Action action) {
        if (getOption(MODE) == Mode.PASTE) {
            SchematicList schematics = mustOption(SCHEMATICS);
            Optional<CachedSchematic> schematic = chooseNext(schematics);
            schematic.ifPresent(this::setClipboard);
        }

        super.secondary(player, pos, action);
    }

    private Path getFilePath() {
        String name = getOption(NAME);
        String dir = getOption(DIR);

        Path root = CopyPasta.getInstance().getConfigDir().resolve("schematics").resolve(dir);
        Path path;

        int counter = 0;
        String fileName = String.format("%s-%03d.schematic", name, counter);
        while (Files.exists(path = root.resolve(fileName))) {
            fileName = String.format("%s-%03d.schematic", name, ++counter);
        }

        return path;
    }

    public Optional<CachedSchematic> chooseNext(SchematicList schematics) {
        for (int i = 0; i < 5; i++) {
            int index = RANDOM.nextInt(schematics.size());
            Optional<CachedSchematic> schematic = schematics.get(index).getSchematic();
            if (schematic.isPresent()) {
                return schematic;
            }
        }
        return Optional.empty();
    }

    private enum Mode {
        PASTE,
        SAVE,
        ;
    }
}