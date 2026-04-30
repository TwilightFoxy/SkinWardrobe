package com.twily.skinwardrobe.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twily.skinwardrobe.SkinWardrobe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MineSkinClient {
    private static final URI GENERATE_URI = URI.create("https://api.mineskin.org/v2/generate");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private MineSkinClient() {
    }

    public static CompletableFuture<SignedSkin> sign(byte[] pngBytes, SkinModel model) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SkinImageValidator.validate(pngBytes);
                HttpRequest request = HttpRequest.newBuilder(GENERATE_URI)
                        .timeout(Duration.ofSeconds(45))
                        .header("User-Agent", "SkinWardrobe/0.4.0")
                        .header("Content-Type", "multipart/form-data; boundary=" + Multipart.BOUNDARY)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(Multipart.skinUpload(pngBytes, model)))
                        .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new SkinRequestException("MineSkin rejected the skin: HTTP " + response.statusCode());
                }

                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("success") && !root.get("success").getAsBoolean()) {
                    throw new SkinRequestException("MineSkin could not sign this skin.");
                }

                JsonObject texture = root.getAsJsonObject("skin")
                        .getAsJsonObject("texture")
                        .getAsJsonObject("data");
                SignedSkin signed = new SignedSkin(
                        texture.get("value").getAsString(),
                        texture.get("signature").getAsString());
                if (!signed.isComplete()) {
                    throw new SkinRequestException("MineSkin returned an incomplete signature.");
                }
                return signed;
            } catch (IOException e) {
                throw new RuntimeException(new SkinRequestException("Could not reach MineSkin.", e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(new SkinRequestException("MineSkin request was interrupted.", e));
            } catch (SkinRequestException e) {
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                SkinWardrobe.LOGGER.warn("Unexpected MineSkin response", e);
                throw new RuntimeException(new SkinRequestException("MineSkin returned an unexpected response.", e));
            }
        });
    }

    public static SkinRequestException unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SkinRequestException skinRequestException) {
                return skinRequestException;
            }
            current = current.getCause();
        }
        return new SkinRequestException("Skin request failed.");
    }

    private static final class Multipart {
        private static final String BOUNDARY = "SkinWardrobe-" + UUID.randomUUID();

        private Multipart() {
        }

        private static byte[] skinUpload(byte[] pngBytes, SkinModel model) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            field(out, "variant", model == SkinModel.SLIM ? "slim" : "classic");
            field(out, "visibility", "unlisted");
            file(out, "file", "skin.png", "image/png", pngBytes);
            out.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }

        private static void field(ByteArrayOutputStream out, String name, String value) throws IOException {
            out.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }

        private static void file(ByteArrayOutputStream out, String name, String filename, String contentType, byte[] bytes) throws IOException {
            out.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
    }
}
