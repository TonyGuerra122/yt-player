package com.tonyguerra.ytplayer.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

public final class VersionUtils {

    public static void startToUpdate(String currentVersion) throws Exception {
        final var url = new URI("https://raw.githubusercontent.com/TonyGuerra122/yt-player/main/pom.xml");

        final var factory = DocumentBuilderFactory.newInstance();
        final var builder = factory.newDocumentBuilder();
        final var doc = builder.parse(url.toURL().openStream());

        doc.getDocumentElement().normalize();

        final var versionNode = doc.getElementsByTagName("version");

        String remoteVersion = null;

        for (int i = 0; i < versionNode.getLength(); i++) {
            final var node = versionNode.item(i);
            if (node.getParentNode().getNodeName().equals("project")) {
                remoteVersion = node.getTextContent();
                break;
            }
        }

        if (remoteVersion == null) {
            throw new RuntimeException("Failed to retrieve the latest version.");
        }

        if (isOutdated(currentVersion, remoteVersion)) {
            final String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                try {
                    final String output = System.getProperty("user.home") + "\\Downloads\\YTPlayer.msi";
                    downloadMsi(output);

                    new ProcessBuilder("msiexec", "/i", output, "/quiet").start();

                    System.exit(0);

                } catch (Exception e) {
                    throw new RuntimeException("Failed to download the latest version.", e);
                }   
            }
        }
    }

    private static void downloadMsi(String output) throws Exception {
        final String url = "https://github.com/TonyGuerra122/yt-player/releases/latest/download/YTPlayer.msi";

        final var outputFile = Path.of(output);

        final var client = HttpClient.newHttpClient();
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(outputFile)).thenAccept(response -> {
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to download the file: " + response.statusCode());
            }
        }).join();
    }

    private static int[] parseVersion(String version) {
        final String core = version.split("-")[0];
        return Arrays.stream(core.split("\\."))
                .mapToInt(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }).toArray();
    }

    private static boolean isOutdated(String current, String latest) {
        final int[] cur = parseVersion(current);
        final int[] lat = parseVersion(latest);

        final int max = Math.max(cur.length, lat.length);
        
        for (int i = 0; i < max; i++) {
            final int c = i < cur.length ? cur[i] : 0;
            final int l = i < lat.length ? lat[i] : 0;
            if (c < l) return true;
            if (c > l) return false;
        }

        final boolean currentIsSnapshot = current.toLowerCase().contains("snapshot");
        final boolean latestIsSnapshot = latest.toLowerCase().contains("snapshot");

        return currentIsSnapshot && !latestIsSnapshot;
    }
}
