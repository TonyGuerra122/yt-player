package com.tonyguerra.ytplayer.constants;

import java.nio.file.Path;

import com.tonyguerra.ytplayer.enums.OSType;

public final class OS {
    public static final OSType CURRENT_OS = OSType.getCurrentOS();
    public static final Path YT_PLAYER_FOLDER = Path.of(System.getProperty("user.home"), "Videos", "yt-player");
    public static final Path YT_DLP_PATH = Path.of(OS.YT_PLAYER_FOLDER.toString(), "bin",
            CURRENT_OS.equals(OSType.WINDOWS) ? "yt-dlp.exe" : "yt-dlp");

}
