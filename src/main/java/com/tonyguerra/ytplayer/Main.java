package com.tonyguerra.ytplayer;

import com.tonyguerra.ytplayer.utils.VersionUtils;

public final class Main {
    public static void main(String[] args) {
        new Thread(() -> {
            final String version = Main.class.getPackage().getImplementationVersion();

            if (version == null) {
                throw new RuntimeException("Version not found.");
            }

            try {
                VersionUtils.startToUpdate(version);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Version Updater Thread").start();
        App.main(args);
    }
}
