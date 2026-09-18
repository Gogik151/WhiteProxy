package ru.white.proxymod.render.sound;

public class GuiSounds {

    private static final String DIR = "gui/";

    public static void open() {
        SoundUtil.playSound_wav(DIR + "gui_open", 0.45F);
    }

    public static void close() {
        SoundUtil.playSound_wav(DIR + "gui_close", 0.45F);
    }

    public static void button() {
        SoundUtil.playSound_wav(DIR + "gui_click", 0.50F);
    }

    public static void toggle(boolean state) {
        SoundUtil.playSound_wav(DIR + (state ? "gui_boolean_enable" : "gui_boolean_disable"), 0.50F);
    }

    public static void picker(boolean ok) {
        SoundUtil.playSound_wav(DIR + "gui_click", 0.40F);
    }
}
