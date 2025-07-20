package com.tonyguerra.ytplayer.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tonyguerra.ytplayer.constants.Mappers;
import com.tonyguerra.ytplayer.constants.OS;
import com.tonyguerra.ytplayer.data.VideoInfo;
import com.tonyguerra.ytplayer.enums.DownloadType;
import com.tonyguerra.ytplayer.enums.OSType;

public final class YtUtils {

    public static void ensureYtDlpInstalled() {
        if (!isYtDlpPresent()) {
            System.out.println("yt-dlp não encontrado, baixando...");
            downloadAndInstallYtDlp();

            if (!isYtDlpPresent()) {
                throw new IllegalStateException("Falha ao instalar o yt-dlp.");
            }
        }
    }

    public static boolean isYtDlpPresent() {
        try {
            final var ytDlpPath = OS.YT_DLP_PATH;

            // Verifica se o arquivo existe
            if (!Files.exists(ytDlpPath)) {
                System.err.println("yt-dlp não encontrado em: " + ytDlpPath);
                return false;
            }

            // Verifica se é executável
            if (!ytDlpPath.toFile().canExecute()) {
                System.err.println("yt-dlp não tem permissão de execução.");
                return false;
            }

            System.out.println("Executando yt-dlp em: " + ytDlpPath);

            final var process = new ProcessBuilder(ytDlpPath.toString(), "--version").start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> System.out.println("yt-dlp > " + line));
            }

            int exitCode = process.waitFor();
            System.out.println("yt-dlp exit code: " + exitCode);
            return exitCode == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void downloadAndInstallYtDlp() {
        try {
            final var osType = OS.CURRENT_OS;
            final String ytDlpUrl = osType.getYtDlpUrl();
            final String fileName = osType.equals(OSType.WINDOWS) ? "yt-dlp.exe" : "yt-dlp";
            final var downloadedPath = Path.of(System.getProperty("java.io.tmpdir")).resolve(fileName);
            final var targetPath = OS.YT_DLP_PATH;

            final Process process;

            if (osType.equals(OSType.LINUX)) {
                process = new ProcessBuilder("curl", "-L", ytDlpUrl, "-o", downloadedPath.toString()).start();
                process.waitFor();
            } else if (osType.equals(OSType.WINDOWS)) {
                process = new ProcessBuilder("powershell", "-Command", "Invoke-WebRequest", ytDlpUrl, "-OutFile",
                        downloadedPath.toString()).start();
                process.waitFor();
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + osType);
            }

            try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            Files.createDirectories(targetPath.getParent());
            Files.move(downloadedPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Adiciona permissão de execução após o move
            if (osType.equals(OSType.LINUX)) {
                if (!targetPath.toFile().setExecutable(true)) {
                    throw new IllegalStateException("Falha ao tornar yt-dlp executável.");
                }
            }

            System.out.println("yt-dlp instalado em: " + targetPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Falha ao instalar o yt-dlp.", e);
        }
    }

    public static Optional<VideoInfo> searchVideo(String url) throws Exception {
        if (!isYtDlpPresent()) {
            throw new IllegalStateException("yt-dlp não está instalado. Por favor, instale-o primeiro.");
        }

        final var process = new ProcessBuilder(
                OS.YT_DLP_PATH.toString(),
                "--dump-single-json",
                "--no-playlist",
                url).start();

        try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                final var errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            final var output = new StringBuilder();
            reader.lines().forEach(output::append);

            process.waitFor();

            if (process.exitValue() != 0) {
                final var errorOutput = new StringBuilder();
                errorReader.lines().forEach(errorOutput::append);
                System.err.println("Erro ao buscar vídeo: " + errorOutput);
                return Optional.empty();
            }

            return Optional.of(Mappers.JSON_MAPPER.readValue(output.toString(), VideoInfo.class));
        } finally {
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        }
    }

    public static String getVideoStreamingUrl(String url) {
        if (!YtUtils.isYtDlpPresent()) {
            throw new IllegalStateException("yt-dlp is not installed. Please install it first.");
        }

        final var processBuilder = new ProcessBuilder(OS.YT_DLP_PATH.toString(), "-f", "18", "-g", url);

        try {
            final var process = processBuilder.start();

            try (
                    final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    final var errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String streamingUrl = reader.readLine();
                final int exitCode = process.waitFor();

                if (exitCode != 0 || streamingUrl == null || streamingUrl.isBlank()) {
                    String errorOutput = errorReader.lines().collect(Collectors.joining("\n"));
                    throw new RuntimeException("Failed to get video streaming URL: " + url + "\n" + errorOutput);
                }

                streamingUrl = streamingUrl.trim();
                return streamingUrl;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while getting video streaming URL: " + url, e);
        }
    }

    public static void downloadVideo(String url, DownloadType downloadType) throws Exception {
        if (!YtUtils.isYtDlpPresent()) {
            throw new IllegalStateException("yt-dlp is not installed. Please install it first.");
        }

        final String format = switch (downloadType) {
            case AUDIO -> "bestaudio";
            case VIDEO -> "bestvideo+bestaudio";
        };

        final var processBuilder = new ProcessBuilder(OS.YT_DLP_PATH.toString(), "-f", format, url);
        final var process = processBuilder.start();

        try (
                var errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                StringBuilder errorOutput = new StringBuilder();
                errorReader.lines().forEach(errorOutput::append);
                throw new RuntimeException("Failed to download video: " + url + "\n" + errorOutput);
            }
        }
    }

}
