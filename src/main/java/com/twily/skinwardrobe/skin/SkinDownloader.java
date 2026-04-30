package com.twily.skinwardrobe.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class SkinDownloader {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private SkinDownloader() {
    }

    public static CompletableFuture<byte[]> downloadPng(String rawUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = URI.create(rawUrl);
                validateHttpUri(uri);
                URI downloadUri = resolveDownloadUri(uri);

                HttpRequest request = HttpRequest.newBuilder(downloadUri)
                        .timeout(Duration.ofSeconds(25))
                        .header("User-Agent", "SkinWardrobe/0.4.0")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2) {
                    throw new SkinRequestException("Could not download skin: HTTP " + response.statusCode());
                }
                byte[] bytes = response.body();
                SkinImageValidator.validate(bytes);
                return bytes;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(new SkinRequestException("Invalid URL.", e));
            } catch (IOException e) {
                throw new RuntimeException(new SkinRequestException("Could not download skin.", e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(new SkinRequestException("Skin download was interrupted.", e));
            } catch (SkinRequestException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static URI resolveDownloadUri(URI uri) throws IOException, InterruptedException, SkinRequestException {
        if (!isYandexDiskPublicUrl(uri)) {
            return uri;
        }

        String apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key="
                + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "SkinWardrobe/0.4.0")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new SkinRequestException("Could not resolve Yandex Disk link: HTTP " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("href")) {
            throw new SkinRequestException("Yandex Disk did not return a download link.");
        }
        URI downloadUri = URI.create(json.get("href").getAsString());
        validateHttpUri(downloadUri);
        return downloadUri;
    }

    private static boolean isYandexDiskPublicUrl(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return host.equals("disk.yandex.ru") || host.equals("yadi.sk") || host.endsWith(".disk.yandex.ru");
    }

    private static void validateHttpUri(URI uri) throws SkinRequestException {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new SkinRequestException("URL must start with http:// or https://.");
        }
    }
}
