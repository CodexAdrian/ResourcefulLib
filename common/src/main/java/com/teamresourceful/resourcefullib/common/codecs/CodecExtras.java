package com.teamresourceful.resourcefullib.common.codecs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.teamresourceful.resourcefullib.common.collections.WeightedCollection;
import com.teamresourceful.resourcefullib.common.exceptions.UtilityClassException;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class CodecExtras {

    private CodecExtras() throws UtilityClassException {
        throw new UtilityClassException();
    }

    public static final PrimitiveCodec<Number> NUMBER = PrimitiveCodecHelper.create(
            DynamicOps::getNumberValue, DynamicOps::createNumeric,
            "Number"
    );

    public static <O, A> Function<O, Optional<A>> optionalFor(final Function<O, @Nullable A> getter) {
        return o -> Optional.ofNullable(getter.apply(o));
    }
    public static <T> Codec<WeightedCollection<T>> weightedCollection(Codec<T> codec, ToDoubleFunction<T> weighter) {
        return codec.listOf().xmap(set -> WeightedCollection.of(set, weighter), collection -> collection.stream().toList());
    }

    public static <T> Codec<T> registryId(Registry<T> registry) {
        return Codec.INT.comapFlatMap(value -> {
            T t = registry.byId(value);
            if (t == null) {
                return DataResult.error(() -> "Unknown registry value: " + value);
            }
            return DataResult.success(t);
        }, registry::getId);
    }

    public static <T> Codec<Set<T>> set(Codec<T> codec) {
        return codec.listOf().xmap(HashSet::new, ArrayList::new);
    }

    public static <T> Codec<Set<T>> linkedSet(Codec<T> codec) {
        return codec.listOf().xmap(LinkedHashSet::new, LinkedList::new);
    }

    public static <T> Codec<T> passthrough(Function<T, JsonElement> encoder, Function<JsonElement, T> decoder) {
        return Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
            if (dynamic.getValue() instanceof JsonElement jsonElement) {
                var output = clearNulls(jsonElement);
                if (output == null) {
                    return DataResult.error(() -> "Value was null for decoder: " + decoder);
                }
                return DataResult.success(decoder.apply(output));
            }
            return DataResult.error(() -> "Value was not an instance of JsonElement");
        }, input -> new Dynamic<>(JsonOps.INSTANCE, clearNulls(encoder.apply(input))));
    }

    public static <S> Codec<S> eitherRight(Codec<Either<S, S>> eitherCodec) {
        return eitherCodec.xmap(e -> e.map(p -> p, p -> p), Either::right);
    }

    public static <S> Codec<S> eitherLeft(Codec<Either<S, S>> eitherCodec) {
        return eitherCodec.xmap(e -> e.map(p -> p, p -> p), Either::left);
    }

    private static JsonElement clearNulls(JsonElement json) {
        if (json instanceof JsonObject object) {
            JsonObject newObject = new JsonObject();
            for (String key : object.keySet()) {
                JsonElement element = clearNulls(object.get(key));
                if (element != null) {
                    newObject.add(key, element);
                }
            }
            return newObject;
        } else if (json instanceof JsonArray array) {
            JsonArray newArray = new JsonArray();
            for (JsonElement item : array) {
                JsonElement element = clearNulls(item);
                if (element != null) {
                    newArray.add(element);
                }
            }
            return newArray;
        }
        return json.isJsonNull() ? null : json;
    }
}
