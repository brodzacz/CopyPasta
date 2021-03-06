package me.dags.copy.block;

import com.flowpowered.math.vector.Vector3i;
import java.util.function.Function;
import java.util.function.Predicate;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.AbstractProperty;
import org.spongepowered.api.data.property.block.HardnessProperty;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.world.World;

/**
 * @author dags <dags@dags.me>
 */
public class BlockUtils {

    private static final Function<PassableProperty, Boolean> PASSABLE = AbstractProperty::getValue;
    private static final Function<HardnessProperty, Boolean> SOFT = h -> h.getValue() != null && h.getValue() < 0.5D;
    private static final Predicate<BlockState> AIR = b ->
            b.getType() == BlockTypes.AIR
            || b.getProperty(PassableProperty.class).map(PASSABLE).orElse(false)
                    || b.getProperty(HardnessProperty.class).map(SOFT).orElse(false);

    public static Vector3i findSolidFoundation(World world, Vector3i pos) {
        return new Vector3i(pos.getX(), findSurfaceY(world, pos), pos.getZ());
    }

    // find the nearest surface block from 'start' position
    // searches upwards & downwards for closest surface position
    public static int findSurfaceY(World world, Vector3i start) {
        boolean wasInSolid = !AIR.test(world.getBlock(start));
        boolean upWasInSolid = wasInSolid;
        boolean downWasInSolid = wasInSolid;

        for (int dy = 1; dy < 256; dy++) {
            int yUp = start.getY() + dy;
            if (yUp < 256) {
                boolean inSolid = !AIR.test(world.getBlock(start.getX(), yUp, start.getZ()));
                if (upWasInSolid && !inSolid) {
                    return yUp - 1;
                }
                upWasInSolid = inSolid;
            }

            int yDown = start.getY() - dy;
            if (yDown >= 0) {
                boolean inSolid = !AIR.test(world.getBlock(start.getX(), yDown, start.getZ()));
                if (!downWasInSolid && inSolid) {
                    return yDown;
                }
                downWasInSolid = inSolid;
            }
        }

        return start.getY();
    }
}
