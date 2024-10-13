package com.teamresourceful.resourcefullib.common.color;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.resourcefullib.common.utils.Scheduling;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class Color {

    protected static final Map<String, Color> colorsWithNames = new HashMap<>();

    public static final Codec<Color> CODEC = Codec.PASSTHROUGH.comapFlatMap(Color::decodeColor, color -> new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive(color.toString())));
    public static final Color DEFAULT = defaultColor();
    public static final Color RAINBOW = createRainbowColor();
    public static final ByteCodec<Color> BYTE_CODEC = ByteCodec.BYTE.dispatch(aByte -> switch (aByte) {
        case 0 -> ByteCodec.unit(DEFAULT);
        case 1 -> ByteCodec.STRING.map(Color::parse, Color::toString);
        default -> ByteCodec.INT.map(Color::new, Color::getValue);
    }, color -> {
        if (color.isDefault()) return (byte) 0;
        if (color.isSpecial()) return (byte) 1;
        return (byte) 2;
    });

    static {
        ConstantColors.init();
    }

    private int r;
    private int g;
    private int b;
    private final int a;
    private int value;

    private boolean defaultValue;

    @Nullable
    private String specialName;

    private float[] rgbaValue;

    //region Constructors

    public Color(int value) {
        this.a = (value >> 24) & 0xFF;
        this.r = (value >> 16) & 0xFF;
        this.g = (value >> 8) & 0xFF;
        this.b = value & 0xFF;
        this.value = value;

        updateFloats();
    }

    public Color(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        updateValue();
        updateFloats();
    }

    //endregion

    //region Special Colors

    private static Color defaultColor() {
        Color color = new Color(0xffffff);
        color.defaultValue = true;
        return color;
    }

    private static Color createRainbowColor() {
        return createPulsingColor("rainbow", 0xff0000, editor -> {
            if (editor.getColor().getIntRed() > 0 && editor.getColor().getIntBlue() == 0) {
                editor.setRed(editor.getColor().getIntRed() - 1);
                editor.setGreen(editor.getColor().getIntGreen() + 1);
            }
            if (editor.getColor().getIntGreen() > 0 && editor.getColor().getIntRed() == 0) {
                editor.setGreen(editor.getColor().getIntGreen() - 1);
                editor.setBlue(editor.getColor().getIntBlue() + 1);
            }
            if (editor.getColor().getIntBlue() > 0 && editor.getColor().getIntGreen() == 0) {
                editor.setRed(editor.getColor().getIntRed() + 1);
                editor.setBlue(editor.getColor().getIntBlue() - 1);
            }
        });
    }

    public static Color createNamedColor(String name, int value) {
        Color color = new Color(value);
        color.specialName = name.toLowerCase(Locale.ENGLISH);
        colorsWithNames.putIfAbsent(color.specialName, color);
        return color;
    }

    public static Color createPulsingColor(String name, int startingValue, Consumer<Color.ColorEditor> editorConsumer) {
        Color color = new Color(startingValue);
        color.specialName = name.toLowerCase(Locale.ENGLISH);
        if (colorsWithNames.containsKey(color.specialName)) return colorsWithNames.get(color.specialName);
        colorsWithNames.put(color.specialName, color);
        Scheduling.schedule(() -> {
            Color.ColorEditor editor = color.new ColorEditor();
            editorConsumer.accept(editor);
            color.updateValue();
            color.updateFloats();
        }, 0, 40, TimeUnit.MILLISECONDS);
        return color;
    }

    //endregion

    //region Parsers

    @Nullable
    public static Color tryParse(String color) {
        if (color.startsWith("0x") || color.startsWith("#") || color.startsWith("0X")) {
            try {
                return new Color(Long.decode(color).intValue());
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else if (colorsWithNames.containsKey(color.toLowerCase())) {
            return colorsWithNames.get(color.toLowerCase());
        }
        return null;
    }

    public static Color parse(String color) {
        Color parsedColor = tryParse(color);
        return parsedColor == null ? DEFAULT : parsedColor;
    }

    public static int parseColor(String color) {
        return parse(color).getValue();
    }

    //endregion

    //region updaters

    private void updateFloats() {
        rgbaValue = new float[4];
        rgbaValue[0] = this.getFloatRed();
        rgbaValue[1] = this.getFloatGreen();
        rgbaValue[2] = this.getFloatBlue();
        rgbaValue[3] = this.getFloatAlpha();
    }

    private void updateValue() {
        this.value = (this.a << 24) | (this.r << 16) | (this.g << 8) | this.b;
    }

    public Color withAlpha(int alpha) {
        return new Color(r, g, b, alpha);
    }

    //endregion

    //region Getters

    //region Float Getters

    public float getFloatRed() {
        return r / 255f;
    }

    public float getFloatGreen() {
        return g / 255f;
    }

    public float getFloatBlue() {
        return b / 255f;
    }

    public float getFloatAlpha() {
        return a / 255f;
    }

    //endregion

    //region Int Getters

    public int getIntRed() {
        return r;
    }

    public int getIntGreen() {
        return g;
    }

    public int getIntBlue() {
        return b;
    }

    public int getIntAlpha() {
        return a;
    }

    //endregion

    public TextColor getTextColor() {
        return TextColor.fromRgb(value);
    }

    public int getValue() {
        return value;
    }

    public boolean isDefault() {
        return defaultValue;
    }

    /**
     * @deprecated Use {@link #isSpecial()} instead.
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "1.21.2")
    public boolean isRainbow() {
        return "rainbow".equals(specialName);
    }

    public boolean isSpecial() {
        return specialName != null;
    }

    @Override
    public String toString() {
        if (specialName != null) return specialName;
        return String.format("#%x", this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultValue, specialName, value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Color color &&
                color.value == this.value &&
                Objects.equals(color.specialName, this.specialName) &&
                color.defaultValue == this.defaultValue;
    }

    public float[] getRGBComponents(float[] compArray) {
        float[] f = compArray == null ? new float[4] : compArray;
        f[0] = rgbaValue[0];
        f[1] = rgbaValue[1];
        f[2] = rgbaValue[2];
        f[3] = rgbaValue[3];
        return f;
    }

    public Style getAsStyle() {
        return Style.EMPTY.withColor(getTextColor());
    }

    //endregion

    //region Codec utils

    public static final Codec<Color> RGB_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("r").orElse(255).forGetter(Color::getIntRed),
            Codec.INT.fieldOf("g").orElse(255).forGetter(Color::getIntGreen),
            Codec.INT.fieldOf("b").orElse(255).forGetter(Color::getIntBlue),
            Codec.INT.fieldOf("a").orElse(255).forGetter(Color::getIntAlpha)
    ).apply(instance, Color::new));

    public static DataResult<Color> decodeColor(Dynamic<?> dynamic) {
        if (dynamic.asNumber().result().isPresent()) {
            return DataResult.success(new Color(dynamic.asInt(0xffffff)));
        } else if (dynamic.asString().result().isPresent()) {
            return DataResult.success(Color.parse(dynamic.asString("WHITE")));
        }
        return RGB_CODEC.parse(dynamic).result().map(DataResult::success)
                .orElse(DataResult.error(() -> "Color input not valid!"));
    }
    //endregion

    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "1.21.2")
    public static void initRainbow() {}

    public class ColorEditor {
        private ColorEditor() {
        }

        public void setRed(int r) {
            Color.this.r = r;
        }

        public void setGreen(int g) {
            Color.this.g = g;
        }

        public void setBlue(int b) {
            Color.this.b = b;
        }

        public Color getColor() {
            return Color.this;
        }
    }
}
