package com.blaublue1303.bigcreateautocrafter.compat.jei;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.runtime.IJeiRuntime;

/** The running JEI instance (set by {@link BcacJeiPlugin}); used by the client smoke test. */
public final class JeiRuntimeHolder {
    @Nullable
    private static volatile IJeiRuntime runtime;

    private JeiRuntimeHolder() {
    }

    @Nullable
    public static IJeiRuntime get() {
        return runtime;
    }

    static void set(@Nullable IJeiRuntime newRuntime) {
        runtime = newRuntime;
    }
}
