package com.tonyguerra.ytplayer.enums;

public enum OSType {
    WINDOWS("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"),
    LINUX("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp");

    private final String ytDlpUrl;

    OSType(String ytDlpUrl) {
        this.ytDlpUrl = ytDlpUrl;
    }

    public String getYtDlpUrl() {
        return ytDlpUrl;
    }

    public static OSType getCurrentOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return LINUX;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
    }
}
