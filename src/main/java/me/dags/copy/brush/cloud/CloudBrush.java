package me.dags.copy.brush.cloud;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import me.dags.copy.CopyPasta;
import me.dags.copy.PlayerManager;
import me.dags.copy.block.Trait;
import me.dags.copy.brush.AbstractBrush;
import me.dags.copy.brush.Aliases;
import me.dags.copy.brush.History;
import me.dags.copy.brush.option.Checks;
import me.dags.copy.brush.option.Option;
import me.dags.copy.brush.option.Parsable;
import me.dags.copy.operation.callback.Callback;
import me.dags.copy.operation.modifier.Filter;
import me.dags.copy.operation.modifier.Translate;
import me.dags.copy.registry.brush.BrushSupplier;
import me.dags.copy.util.fmt;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.entity.living.player.Player;

import java.util.*;

/**
 * @author dags <dags@dags.me>
 */
@Aliases({"cloud"})
public class CloudBrush extends AbstractBrush implements Parsable {

    public static final Option<Integer> SEED = Option.of("seed", 8008);
    public static final Option<Integer> SCALE = Option.of("scale", 24, Checks.range(2, 256));
    public static final Option<Integer> OCTAVES = Option.of("octaves", 4, Checks.range(1, 8));
    public static final Option<Integer> RADIUS = Option.of("radius", 24, Checks.range(1, 96));
    public static final Option<Integer> HEIGHT = Option.of("height", 9, Checks.range(1, 48));
    public static final Option<Integer> OFFSET = Option.of("offset", 4, Checks.range(1, 16));
    public static final Option<Float> DETAIL = Option.of("detail", 2F, Checks.range(0.5F, 5.0F));
    public static final Option<Float> DENSITY = Option.of("density", 0.15F, Checks.range(0F, 1F));
    public static final Option<Float> FEATHER = Option.of("feather", 0.5F, Checks.range(0F, 1F));
    public static final Option<Boolean> REPLACE_AIR = Option.of("air.replace", true);
    public static final Option<BlockType> MATERIAL = Trait.MATERIAL_OPTION;
    public static final Option<Trait> TRAIT = Trait.TRAIT_OPTION;

    private List<BlockState> materials = Collections.emptyList();
    private BlockType type = BlockTypes.AIR;
    private Trait trait = new Trait("none");
    private float density = 0F;

    private CloudBrush() {
        super(5);
        setOption(RANGE, 32);
    }

    @Override
    public List<Option<?>> getParseOptions() {
        return Arrays.asList(SCALE, OCTAVES, RADIUS, HEIGHT, OFFSET, DETAIL, DENSITY, FEATHER);
    }

    @Override
    public void apply(Player player, Vector3i pos, History history) {
        BlockType type = getOption(MATERIAL);
        Trait trait = getOption(TRAIT);
        float density = getOption(DENSITY);

        if (type != this.type || !trait.equals(this.trait) || density != this.density) {
            this.type = type;
            this.trait = trait;
            this.density = density;
            this.materials = getVariants(type, trait.getName(), density);
        }

        if (materials.size() <= 0) {
            fmt.error("No materials match block: %s, trait: %s", type, trait).tell(player);
            return;
        }

        Cloud cloud = new Cloud(this, materials);
        Filter filter = Filter.replaceAir(getOption(REPLACE_AIR));
        PlayerManager.getInstance().must(player).setOperating(true);
        Callback callback = Callback.of(player, history, filter, Filter.ANY, Translate.NONE);
        Runnable task = cloud.createTask(player.getUniqueId(), pos, callback);
        CopyPasta.getInstance().submitAsync(task);
    }

    public static BrushSupplier supplier() {
        return p -> new CloudBrush();
    }

    private static List<BlockState> getVariants(BlockType type, String trait, float density) {
        return type.getTrait(trait).map(t -> getVariants(type, t, density)).orElse(Collections.emptyList());
    }

    private static List<BlockState> getVariants(BlockType type, BlockTrait<?> trait, float density) {
        ImmutableList.Builder<BlockState> builder = ImmutableList.builder();
        Collection<? extends Comparable<?>> values = trait.getPossibleValues();

        // pad with air blocks according to the density
        for (int i = Math.round(values.size() * (1 - density)); i > 0; i--) {
            builder.add(BlockTypes.AIR.getDefaultState());
        }

        // fill with variants of the given trait, sorted
        BlockState baseState = type.getDefaultState();
        values.stream()
                .sorted()
                .map(value -> baseState.withTrait(trait, value))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(builder::add);

        return builder.build();
    }
}
