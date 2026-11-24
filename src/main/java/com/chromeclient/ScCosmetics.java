package com.chromeclient;

/**
 * The mod id and the cosmetics start-up. The cosmetics classes stay in com.chromeclient because the SC menu mod
 * (ChromePoly) looks them up by these names for its wardrobe and lobby character.
 */
public final class ScCosmetics {
    public static final String MOD_ID = "scmod";

    private ScCosmetics() {}

    public static void init() {
        Cosmetics.load();
        Cosmetics.init();
        CosmeticSync.init();
    }
}
