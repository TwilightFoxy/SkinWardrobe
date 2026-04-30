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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkinDownloader {
    private static final Pattern ELY_BY_SKIN_URL = Pattern.compile(
            "alight\\.service\\.skin\\s*=\\s*\\{.*?\"skin_url\"\\s*:\\s*\"(https:\\\\/\\\\/ely\\.by\\\\/storage\\\\/skins\\\\/[^\"\\\\]+\\.png)\"",
            Pattern.DOTALL);
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
                        .header("User-Agent", "SkinWardrobe/0.6.0")
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
        if (isYandexDiskPublicUrl(uri)) {
            return resolveYandexDiskDownloadUri(uri);
        }
        if (isElyBySkinPageUrl(uri)) {
            return resolveElyBySkinUri(uri);
        }
        return uri;
    }

    private static URI resolveYandexDiskDownloadUri(URI uri) throws IOException, InterruptedException, SkinRequestException {
        String apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key="
                + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "SkinWardrobe/0.6.0")
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

    private static URI resolveElyBySkinUri(URI uri) throws IOException, InterruptedException, SkinRequestException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "SkinWardrobe/0.6.0")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new SkinRequestException("Could not resolve Ely.by skin page: HTTP " + response.statusCode());
        }

        Matcher matcher = ELY_BY_SKIN_URL.matcher(response.body());
        if (!matcher.find()) {
            throw new SkinRequestException("Ely.by did not return a skin PNG link.");
        }
        URI downloadUri = URI.create(matcher.group(1).replace("\\/", "/"));
        validateHttpUri(downloadUri);
        return downloadUri;
    }

    private static boolean isYandexDiskPublicUrl(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return host.equals("disk.yandex.ru") || host.equals("yadi.sk") || host.endsWith(".disk.yandex.ru");
    }

    private static boolean isElyBySkinPageUrl(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        return (host.equals("ely.by") || host.equals("www.ely.by")) && path.matches("/skins/s\\d+");
    }

    private static void validateHttpUri(URI uri) throws SkinRequestException {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new SkinRequestException("URL must start with http:// or https://.");
        }
    }
}
