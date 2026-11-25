package com.scclient;

import com.chromeclient.ScCosmetics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SC Client's in-game mod: the Wardrobe cosmetics and the pay timer (in the Right Shift settings). Nothing else. */
public final class ScMod implements ClientModInitializer {
    /** saved in config/sc-mod.json */
    public static final class Settings {
        public int payTimerSeconds = 300;
        public String payRegex = "";
        public boolean seeHats = true, seeCapes = true, seeWings = true;
        /** the menu mod's badge in front of other players' names (off by default) */
        public boolean showBadges = false;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings settings = new Settings();
    private static KeyMapping timerKey;

    public static Settings settings() { return settings; }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("sc-mod.json"); }

    private static void load() {
        try {
            Path f = file();
            if (Files.isRegularFile(f)) {
                Settings s = GSON.fromJson(Files.readString(f), Settings.class);
                if (s != null) settings = s;
            }
        } catch (Exception ignored) { }
        settings.payTimerSeconds = Math.max(5, Math.min(99999, settings.payTimerSeconds));
        if (settings.payRegex == null) settings.payRegex = "";
        save();
    }

    public static void save() {
        try { Files.writeString(file(), GSON.toJson(settings)); } catch (Exception ignored) { }
    }

    @Override
    public void onInitializeClient() {
        ScCosmetics.init();
        load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ScCosmetics.MOD_ID, "main"));
        timerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key." + ScCosmetics.MOD_ID + ".paytimer", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));

        // the pay timer reads server messages ("Steve paid you $500") while it runs
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) PayTimer.get().onMessage(message.getString());
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ScCosmetics.MOD_ID, "paytimer"), PayTimerHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PayTimer.get().update(client);
            if (++ticks % 40 == 1) applyBadges();
            while (timerKey.consumeClick()) {
                if (client.player == null) continue;
                PayTimer pt = PayTimer.get();
                if (pt.running) pt.stop();
                else if (pt.finished) pt.clear();
                else pt.start(settings.payTimerSeconds);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("paytimer")
                        .executes(ctx -> { ctx.getSource().sendFeedback(Component.literal(help())); return 1; })
                        .then(ClientCommandManager.literal("start")
                                .executes(ctx -> { PayTimer.get().start(settings.payTimerSeconds); return 1; })
                                .then(ClientCommandManager.argument("time", StringArgumentType.word())
                                        .executes(ctx -> {
                                            int s = seconds(StringArgumentType.getString(ctx, "time"));
                                            if (s <= 0) { ctx.getSource().sendError(Component.literal("Write the time like 5m, 90s or 1m30s.")); return 0; }
                                            settings.payTimerSeconds = s; save();
                                            PayTimer.get().start(s);
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("stop").executes(ctx -> { PayTimer.get().stop(); return 1; }))
                        .then(ClientCommandManager.literal("clear").executes(ctx -> { PayTimer.get().clear(); return 1; }))
                        .then(ClientCommandManager.literal("length")
                                .then(ClientCommandManager.argument("time", StringArgumentType.word())
                                        .executes(ctx -> {
                                            int s = seconds(StringArgumentType.getString(ctx, "time"));
                                            if (s <= 0) { ctx.getSource().sendError(Component.literal("Write the time like 5m, 90s or 1m30s.")); return 0; }
                                            settings.payTimerSeconds = s; save();
                                            ctx.getSource().sendFeedback(Component.literal("[SC Client] Pay timer length: " + PayTimer.time(s * 1000L)));
                                            return 1;
                                        })))));
    }

    private static int ticks = 0;
    private static java.lang.reflect.Method badgeSetter;
    private static boolean badgeLookedUp = false;

    /** keeps the menu mod's name badges the way the Render setting says (the menu mod is found by name; without it nothing happens) */
    private static void applyBadges() {
        try {
            if (!badgeLookedUp) {
                badgeLookedUp = true;
                badgeSetter = Class.forName("org.polyfrost.polyplus.client.PolyPlusConfig").getMethod("setShowPolyPlusIndicator", boolean.class);
            }
            if (badgeSetter != null) badgeSetter.invoke(null, settings.showBadges);
        } catch (Throwable ignored) {
            badgeSetter = null;
        }
    }

    private static String help() {
        return "[SC Client] Pay timer (" + PayTimer.time(settings.payTimerSeconds * 1000L) + "). Start / stop it in the settings (Right Shift > HUD), or: "
                + "/paytimer start [5m], /paytimer stop, /paytimer clear, /paytimer length <1m30s>";
    }

    private static final Pattern PART = Pattern.compile("(\\d+)([hms]?)");

    /** "5m" -> 300, "1m30s" -> 90, "90" -> 90 (seconds); 0 when it can't be read */
    static int seconds(String text) {
        String t = text.trim().toLowerCase();
        if (!t.matches("(\\d+[hms]?)+")) return 0;
        long total = 0;
        Matcher m = PART.matcher(t);
        while (m.find()) {
            long n = Long.parseLong(m.group(1));
            total += switch (m.group(2)) { case "h" -> n * 3600; case "m" -> n * 60; default -> n; };
        }
        return total < 5 || total > 99999 ? 0 : (int) total;
    }
}
