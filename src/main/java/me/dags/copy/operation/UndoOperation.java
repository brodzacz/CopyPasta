package me.dags.copy.operation;

import me.dags.commandbus.fmt.Fmt;
import me.dags.copy.CopyPasta;
import me.dags.copy.brush.History;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.world.BlockChangeFlag;

import java.util.List;
import java.util.UUID;

/**
 * @author dags <dags@dags.me>
 */
public class UndoOperation implements Operation {

    private final List<BlockSnapshot> snapshots;
    private final History history;
    private final UUID owner;

    public UndoOperation(List<BlockSnapshot> snapshots, UUID owner, History history) {
        this.snapshots = snapshots;
        this.history = history;
        this.owner = owner;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void calculate() {

    }

    @Override
    public void test() {

    }

    @Override
    public void apply() {
        for (BlockSnapshot snapshot : snapshots) {
            snapshot.restore(true, BlockChangeFlag.NONE);
        }

        Sponge.getServer().getPlayer(owner).ifPresent(player -> {
            int size = history.getSize();
            int max = history.getMax();
            Fmt.subdued("Undo Complete (%s / %s)", size, max).tell(CopyPasta.NOTICE_TYPE, player);
        });
    }

    @Override
    public void dispose() {
        CopyPasta.getInstance().getData(owner).ifPresent(data -> data.setOperating(false));
    }
}
