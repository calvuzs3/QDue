package net.calvuz.qdue.core.common;

import android.content.Context;

public class Lib {

    /**
     * Check if this is a debug build
     */
    public static boolean isDebugBuild(Context context) {
        try {
            return (context.getApplicationInfo().flags &
                    android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }
}
