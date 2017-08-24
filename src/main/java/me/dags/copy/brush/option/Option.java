package me.dags.copy.brush.option;

import me.dags.commandbus.utils.ClassUtils;
import me.dags.copy.util.Serializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author dags <dags@dags.me>
 */
public class Option<T> {

    private final String key;
    private final Class<T> type;
    private final Supplier<Value<T>> defaultValue;

    private Option(@Nonnull String key, @Nonnull Class<T> type, @Nonnull Supplier<Value<T>> defaultValue) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        register();
    }

    private void register() {
        Value value = defaultValue.get();
        if (value.isPresent()) {
            Object o = value.get();
            if (o instanceof Serializable) {
                Serializable.class.cast(o).register(TypeSerializers.getDefaultSerializers());
            }
        }
    }

    public String getId() {
        return key;
    }

    public Class<T> getType() {
        return type;
    }

    public Value<T> getDefault() {
        return defaultValue.get();
    }

    public boolean accepts(Object o) {
        return o != null && getType().isInstance(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option<?> option = (Option<?>) o;
        return getType() == option.getType() && key.equals(option.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }

    public static <T> Option<T> of(String key, T value) {
        return of(key, wrap(value.getClass()), () -> value);
    }

    public static <T> Option<T> of(String key, Class<T> type, Supplier<T> supplier) {
        Class<T> t = wrap(type);
        Supplier<Value<T>> s = () -> Value.of(supplier.get());
        return new Option<>(key, t, s);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> wrap(Class<?> type) {
        return (Class<T>) ClassUtils.wrapPrimitive(type);
    }
}
