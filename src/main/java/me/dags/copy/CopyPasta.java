package me.dags.copy;

import com.google.inject.Inject;
import me.dags.commandbus.CommandBus;
import me.dags.commandbus.element.ElementFactory;
import me.dags.copy.block.Mappers;
import me.dags.copy.brush.clipboard.ClipboardBrush;
import me.dags.copy.brush.multi.MultiPointBrush;
import me.dags.copy.brush.option.Option;
import me.dags.copy.brush.option.Value;
import me.dags.copy.brush.schematic.SchematicBrush;
import me.dags.copy.command.BrushCommands;
import me.dags.copy.command.element.BrushElement;
import me.dags.copy.command.element.OptionElement;
import me.dags.copy.command.element.ValueElement;
import me.dags.copy.operation.OperationManager;
import me.dags.copy.registry.brush.BrushRegistry;
import me.dags.copy.registry.brush.BrushType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = CopyPasta.ID, name = "CopyPasta", version = "0.3", description = ".")
public class CopyPasta {

    public static final String ID = "copypasta";
    public static final ChatType NOTICE_TYPE = ChatTypes.ACTION_BAR;

    private static CopyPasta instance;

    private final Path configDir;
    private final EventListener eventListener = new EventListener();
    private final OperationManager operationManager = new OperationManager();

    private SpongeExecutorService asyncExecutor;

    @Inject
    public CopyPasta(@ConfigDir(sharedRoot = false) Path configDir) {
        CopyPasta.instance = this;
        this.configDir = configDir;
    }

    @Listener
    public void pre(GamePreInitializationEvent event) {
        BrushRegistry.getInstance().register(ClipboardBrush.class, ClipboardBrush.supplier());
        BrushRegistry.getInstance().register(SchematicBrush.class, SchematicBrush.supplier());
        BrushRegistry.getInstance().register(MultiPointBrush.class, MultiPointBrush.supplier());
        asyncExecutor = Sponge.getScheduler().createAsyncExecutor(this);
    }

    @Listener
    public void init(GameInitializationEvent event) {
        reload(null);

        ElementFactory factory = CommandBus.elements()
                .provider(BrushType.class, BrushElement.provider())
                .provider(Option.class, OptionElement.provider())
                .provider(Value.class, ValueElement.provider())
                .build();

        CommandBus commandBus = CommandBus.builder()
                .elements(factory)
                .owner(this)
                .build();

        commandBus.register(BrushCommands.class).submit();

        Mappers.init();
    }

    @Listener
    public void reload(GameReloadEvent event) {
        stop(null);
        Sponge.getEventManager().unregisterListeners(eventListener);
        Sponge.getEventManager().registerListeners(this, eventListener);
        Task.builder().execute(operationManager).intervalTicks(1).submit(this);
    }

    @Listener
    public void stop(GameStoppingServerEvent event) {
        Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

        try {
            asyncExecutor.shutdown();
            asyncExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        operationManager.finish();
        operationManager.reset();
    }

    public Path getConfigDir() {
        return configDir;
    }

    public OperationManager getOperationManager() {
        return operationManager;
    }

    public void submitAsync(Runnable runnable) {
        asyncExecutor.submit(runnable);
    }

    public static CopyPasta getInstance() {
        return instance;
    }
}
