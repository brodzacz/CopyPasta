package me.dags.copy.command.element;

import me.dags.commandbus.CommandBus;
import me.dags.commandbus.command.CommandException;
import me.dags.commandbus.element.ChainElement;
import me.dags.commandbus.element.ElementFactory;
import me.dags.commandbus.element.ElementProvider;
import me.dags.commandbus.element.function.Filter;
import me.dags.commandbus.element.function.Options;
import me.dags.commandbus.element.function.ValueParser;
import me.dags.copy.CopyPasta;
import me.dags.copy.brush.option.Option;
import me.dags.copy.brush.option.Value;
import me.dags.copy.brush.stencil.StencilPalette;
import me.dags.copy.registry.brush.BrushRegistry;
import me.dags.copy.registry.brush.BrushType;
import org.spongepowered.api.block.BlockState;

import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
public class BrushElements {

    private static final ElementFactory builtin = CommandBus.elements().build();

    public static CommandBus getCommandBus(CopyPasta plugin) {
        return CommandBus.builder()
                .elements(getElements())
                .owner(plugin)
                .build();
    }

    private static ElementFactory getElements() {
        ElementFactory.Builder builder = CommandBus.elements();
        stencilPalette(builder);

        brush(builder);
        option(builder);
        value(builder);

        return builder.build();
    }

    private static void brush(ElementFactory.Builder builder) {
        builder.options(BrushType.class, BrushRegistry.getInstance()::getAliases);
        builder.filter(BrushType.class, Filter.STARTS_WITH);
        builder.parser(BrushType.class, s -> {
            Optional<BrushType> type = BrushRegistry.getInstance().getById(s);
            if (!type.isPresent()) {
                throw new CommandException("Invalid BrushType '%s'", s);
            }
            return type.get();
        });
    }

    // needs to be added before ValueElement
    private static void stencilPalette(ElementFactory.Builder builder) {
        final ValueParser<?> stateParser = builtin.getParser(BlockState.class);
        final ValueParser<?> weightParser = builtin.getParser(double.class);
        final Options stateOptions = builtin.getOptions(BlockState.class);
        final Filter stateFilter = builtin.getFilter(BlockState.class);

        ElementProvider provider = (key, priority, options, filter, parser) -> {
            ChainElement.Builder<StencilPalette, StencilPalette> chainBuilder = ChainElement.builder();
            chainBuilder.key(key);
            chainBuilder.dependency(StencilPalette.class);
            chainBuilder.filter(stateFilter);
            chainBuilder.options(palette -> stateOptions.get());
            chainBuilder.mapper((input, palette) -> {
                BlockState state = (BlockState) stateParser.parse(input);
                Double weight = 1D;
                if (input.hasNext()) {
                    weight = (Double) weightParser.parse(input);
                }
                return palette.add(state, weight);
            });
            return new StencilPaletteElement(key, chainBuilder);
        };

        builder.provider(StencilPalette.class, provider);
    }

    private static void option(ElementFactory.Builder builder) {
        ElementProvider provider = (key, priority, options, filter, parser) -> {
            ChainElement.Builder<BrushType, Option> chainBuilder = ChainElement.builder();
            chainBuilder.key(key)
                    .filter(filter)
                    .dependency(BrushType.class)
                    .options(type -> type.getOptions().stream().map(Option::getId))
                    .mapper((input, type) -> {
                        String next = input.next();
                        Optional<Option<?>> option = type.getOption(next);
                        if (!option.isPresent()) {
                            throw new CommandException("Invalid Option '%s' for Brush %s", next, type);
                        }
                        return option.get();
                    });
            return new OptionElement(chainBuilder);
        };

        builder.provider(Option.class, provider);
    }

    private static void value(ElementFactory.Builder builder) {
        ElementFactory factory = builder.build();

        ElementProvider provider = (key, priority, options, filter, parser) -> ChainElement.<Option, Value<?>>builder()
                .key(key)
                .filter(Filter.CONTAINS)
                .dependency(Option.class)
                .options(option -> factory.getOptions(option.getType()).get())
                .mapper((input, option) -> {
                    ValueParser<?> p = factory.getParser(option.getType());
                    Object value = p.parse(input);
                    if (value == null) {
                        throw new CommandException("Unable to parse value for '%s'", option);
                    }
                    return Value.of(value);
                })
                .build();

        builder.provider(Value.class, provider);
    }
}
