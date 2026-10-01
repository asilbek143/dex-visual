package uz.dexvisual;

import net.minecraft.util.math.MathHelper;

public final class Theme {
    public static final String[] NAMES = {"Ocean", "Purple", "Sunset", "Mint", "Rainbow"};
    private static final int[][] PAL = {
            {0x22D3EE, 0x6366F1},
            {0xA855F7, 0xEC4899},
            {0xF97316, 0xEC4899},
            {0x34D399, 0x22D3EE}
    };

    public static int at(double t) {
        double p = ((t % 1.0) + 1.0) % 1.0;
        int m = Mods.THEME.idx;
        if (m >= PAL.length) {
            return 0xFF000000 | (MathHelper.hsvToRgb((float) p, 0.55f, 1f) & 0xFFFFFF);
        }
        double ping = p < 0.5 ? p * 2 : (1 - p) * 2;
        return Draw.lerp(0xFF000000 | PAL[m][0], 0xFF000000 | PAL[m][1], (float) ping);
    }

    /** Animated gradient color, offset shifts position along the gradient. */
    public static int grad(double off) {
        return at(System.nanoTime() / 1.0e9 * 0.2 + off);
    }

    private Theme() {}
}
