package com.chromeclient;

import com.scclient.PayTimer;
import com.scclient.ScMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * The SC settings pages inside the menu mod's Right Shift menu (HUD / Render / Music). The menu mod looks this class up
 * by name and draws the modules described here; the name and the map keys must stay as they are.
 */
public final class ChromeMenuApi {
    private ChromeMenuApi() {}

    private static final BooleanSupplier ALWAYS = () -> true;

    private static Map<String, Object> module(String tab, String name, String desc, List<Map<String, Object>> opts) {
        Map<String, Object> m = new HashMap<>();
        m.put("tab", tab);
        m.put("name", name);
        m.put("desc", desc);
        m.put("opts", opts);
        return m;
    }

    private static Map<String, Object> text(Supplier<String> label) {
        Map<String, Object> o = new HashMap<>();
        o.put("kind", "TEXT");
        o.put("label", label);
        o.put("vis", ALWAYS);
        return o;
    }

    private static Map<String, Object> action(Supplier<String> label, Runnable act) {
        Map<String, Object> o = new HashMap<>();
        o.put("kind", "ACTION");
        o.put("label", label);
        o.put("vis", ALWAYS);
        o.put("act", act);
        return o;
    }

    private static Map<String, Object> toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        Map<String, Object> o = new HashMap<>();
        o.put("kind", "TOGGLE");
        o.put("label", (Supplier<String>) () -> label);
        o.put("vis", ALWAYS);
        o.put("getB", get);
        o.put("setB", set);
        return o;
    }

    private static Map<String, Object> slider(String label, DoubleSupplier get, Consumer<Double> set, double min, double max, String fmt) {
        Map<String, Object> o = new HashMap<>();
        o.put("kind", "SLIDER");
        o.put("label", (Supplier<String>) () -> label);
        o.put("vis", ALWAYS);
        o.put("getD", get);
        o.put("setD", set);
        o.put("min", min);
        o.put("max", max);
        o.put("fmt", fmt);
        return o;
    }

    /** +by when the menu asked for more than now, -by when it asked for less */
    private static int step(double asked, int now, int by) {
        return asked > now + 0.001 ? by : asked < now - 0.001 ? -by : 0;
    }

    public static List<Map<String, Object>> modules() {
        ScMod.Settings s = ScMod.settings();
        List<Map<String, Object>> out = new ArrayList<>();

        List<Map<String, Object>> timer = new ArrayList<>();
        timer.add(text(() -> {
            PayTimer pt = PayTimer.get();
            return "Status: " + (pt.isRunning() ? PayTimer.time(pt.remainingMs()) : (pt.isFinished() ? "Done" : "Ready"));
        }));
        timer.add(action(() -> PayTimer.get().isRunning() ? "Stop timer" : "Start timer", () -> {
            PayTimer pt = PayTimer.get();
            if (pt.isRunning()) pt.stop(); else pt.start(s.payTimerSeconds);
        }));
        timer.add(action(() -> "Hide the result", () -> PayTimer.get().clear()));
        // the menu's - / + buttons move a slider by a twentieth of its range (3 here), so only the direction is used:
        // minutes go up / down by 1, seconds by 2
        timer.add(slider("Minutes", () -> s.payTimerSeconds / 60, v -> {
            int min = s.payTimerSeconds / 60, sec = s.payTimerSeconds % 60;
            min = Math.max(0, Math.min(60, min + step(v, min, 1)));
            s.payTimerSeconds = Math.max(5, min * 60 + sec);
        }, 0, 60, "%d"));
        timer.add(slider("Seconds", () -> s.payTimerSeconds % 60, v -> {
            int min = s.payTimerSeconds / 60, sec = s.payTimerSeconds % 60;
            sec = Math.max(0, Math.min(59, sec + step(v, sec, 2)));
            s.payTimerSeconds = Math.max(5, min * 60 + sec);
        }, 0, 59, "%d"));
        out.add(module("HUD", "Pay timer", "Counts down and reads payment messages in chat. It shows in the top-left corner.", timer));

        List<Map<String, Object>> see = new ArrayList<>();
        see.add(toggle("Other players' hats", () -> s.seeHats, v -> s.seeHats = v));
        see.add(toggle("Other players' capes", () -> s.seeCapes, v -> s.seeCapes = v));
        see.add(toggle("Other players' wings", () -> s.seeWings, v -> s.seeWings = v));
        out.add(module("Render", "Cosmetics", "Pick yours in the launcher's Wardrobe. Here you choose what you see on others.", see));

        out.add(module("Music", "Music", "SC Client plays Minecraft's own music.", new ArrayList<>()));
        return out;
    }

    public static void save() {
        ScMod.save();
    }
}
