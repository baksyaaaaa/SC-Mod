package com.scclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** The pay timer box in the top-left corner while the timer runs (and its result after). */
public final class PayTimerHud {
    private static final int BG = 0xB00B0816, BORDER = 0x668B5CF6, ACCENT = 0xFF8B5CF6, TEXT = 0xFFFFFFFF, MUTED = 0xFFB7ABD6, TRACK = 0x33FFFFFF;

    private PayTimerHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        PayTimer pt = PayTimer.get();
        if (!pt.visible()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        String time = pt.running ? PayTimer.time(pt.remainingMs()) : "Done";
        String best = pt.count == 0 ? "none yet" : PayTimer.money(pt.best) + (pt.bestFrom.isEmpty() ? "" : "  " + pt.bestFrom);
        String total = PayTimer.money(pt.total) + "  (" + pt.count + ")";
        int w = Math.max(124, Math.max(font.width("Best " + best), font.width("Total " + total)) + 14);
        int h = pt.count > 0 ? 58 : 47;
        int x = 6, y = 6;

        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);

        g.fill(x + 6, y + 7, x + 10, y + 11, pt.running ? ACCENT : MUTED);
        g.drawString(font, "PAY TIMER", x + 14, y + 5, MUTED, false);
        g.drawString(font, time, x + 6, y + 17, TEXT, true);

        float k = pt.running && pt.totalMs > 0 ? 1f - Math.max(0f, Math.min(1f, pt.remainingMs() / (float) pt.totalMs)) : 1f;
        int barW = w - 12;
        g.fill(x + 6, y + 29, x + 6 + barW, y + 31, TRACK);
        if (k > 0.01f) g.fill(x + 6, y + 29, x + 6 + Math.max(2, Math.round(barW * k)), y + 31, ACCENT);

        g.drawString(font, "Best", x + 6, y + 36, MUTED, false);
        g.drawString(font, best, x + 6 + font.width("Best "), y + 36, TEXT, false);
        if (pt.count > 0) {
            g.drawString(font, "Total", x + 6, y + 47, MUTED, false);
            g.drawString(font, total, x + 6 + font.width("Total "), y + 47, TEXT, false);
        }
    }
}
