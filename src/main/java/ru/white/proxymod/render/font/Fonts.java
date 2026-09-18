package ru.white.proxymod.render.font;

import java.util.LinkedHashMap;
import java.util.Map;

public class Fonts {

    private static final Map<String, String> FONT_REGISTRY = new LinkedHashMap<>();

    public static final Font sf_bold = register("sf_bold", "sf_bold");
    public static final Font sf_medium = register("sf_medium", "sf_medium");
    public static final Font sf_regular = register("sf_regular", "sf_regular");

    private static Font register(String name, String path) {
        FONT_REGISTRY.put(name, path);
        return new Font(name);
    }

    public static Map<String, String> getRegistry() {
        return FONT_REGISTRY;
    }

    private Fonts() {}
}
