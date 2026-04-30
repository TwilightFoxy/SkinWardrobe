package com.twily.skinwardrobe.skin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class SkinImageValidator {
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    private SkinImageValidator() {
    }

    public static Result validate(byte[] bytes) throws SkinRequestException {
        if (bytes.length == 0) {
            throw new SkinRequestException("The image is empty.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new SkinRequestException("The image is too large. Maximum size is 2 MiB.");
        }
        if (!isPng(bytes)) {
            throw new SkinRequestException("Only PNG files are supported.");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new SkinRequestException("Could not read the PNG image.", e);
        }

        if (image == null) {
            throw new SkinRequestException("Could not read the PNG image.");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (width == 64 && (height == 64 || height == 32)) {
            return new Result(width, height);
        }

        throw new SkinRequestException("Skin PNG must be 64x64 or 64x32 pixels.");
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    public record Result(int width, int height) {
    }
}
