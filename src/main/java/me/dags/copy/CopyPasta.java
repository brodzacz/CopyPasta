package me.dags.copy;

import com.google.inject.Inject;
import me.dags.commandbus.CommandBus;
import me.dags.commandbus.element.ElementFactory;
import me.dags.copy.block.Mappers;
import me.dags.copy.brush.clipboard.ClipboardBrush;
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
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = "copypasta", name = "CopyPasta", version = "0.2", description = ".")
public class CopyPasta {

    public static final ChatType CHAT_TYPE = ChatTypes.CHAT;
    public static final ChatType NOTICE_TYPE = ChatTypes.ACTION_BAR;
    private static CopyPasta instance;

    private final Path configDir;
    private final PluginContainer container;
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final EventListener eventListener = new EventListener();
    private final OperationManager operationManager = new OperationManager();

    private SpongeExecutorService asyncExecutor;

    @Inject
    public CopyPasta(PluginContainer container, @ConfigDir(sharedRoot = false) Path configDir) {
        CopyPasta.instance = this;
        this.configDir = configDir;
        this.container = container;
    }

    @Listener
    public void pre(GamePreInitializationEvent event) {
        Sponge.getRegistry().registerModule(BrushType.class, BrushRegistry.getInstance());
        BrushRegistry.getInstance().register(ClipboardBrush.class, ClipboardBrush::new);
        BrushRegistry.getInstance().register(SchematicBrush.class, SchematicBrush::new);
    }

    @Listener
    public void init(GameInitializationEvent event) {
        asyncExecutor = Sponge.getScheduler().createAsyncExecutor(this);
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

    public PlayerData ensureData(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(configDir.resolve("todo.conf")));
    }

    public Optional<PlayerData> getData(Player player) {
        return getData(player.getUniqueId());
    }

    public Optional<PlayerData> getData(UUID uuid) {
        return Optional.ofNullable(data.get(uuid));
    }

    public void dropData(Player player) {
        dropData(player.getUniqueId());
    }

    public void dropData(UUID uuid) {
        data.remove(uuid);
    }

    public Cause getCause(Player player) {
        return Cause.source(container)
                .notifier(player)
                .owner(player)
                .build();
    }

    public void submitAsync(Runnable runnable) {
        asyncExecutor.submit(runnable);
    }

    public static CopyPasta getInstance() {
        return instance;
    }
}
