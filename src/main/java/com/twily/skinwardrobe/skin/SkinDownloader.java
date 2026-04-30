package com.twily.skinwardrobe.skin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (!scheme.equals("https") && !scheme.equals("http")) {
                    throw new SkinRequestException("URL must start with http:// or https://.");
                }

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(25))
                        .header("User-Agent", "SkinWardrobe/0.1.0")
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
}
