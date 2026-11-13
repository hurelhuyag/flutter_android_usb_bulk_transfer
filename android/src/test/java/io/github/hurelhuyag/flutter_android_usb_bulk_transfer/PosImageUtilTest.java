package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.github.hurelhuyag.flutter_android_usb_bulk_transfer.PosImageUtil.Dithering;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class PosImageUtilTest {

    private static final int BAND = 256;

    private static int grey(int luminance) {
        return 0xFF000000 | luminance << 16 | luminance << 8 | luminance;
    }

    private static int[] filled(int width, int height, int argb) {
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, argb);
        return pixels;
    }

    /// Whether dot (x, y) prints black, skipping ESC @ and the 8-byte GS v 0 header in front of each 256-row band.
    private static boolean black(byte[] out, int width, int x, int y) {
        int bytesPerRow = (width + 7) / 8;
        int index = 2 + (y / BAND + 1) * 8 + y * bytesPerRow + x / 8;
        return (out[index] & (0x80 >> (x % 8))) != 0;
    }

    private static int countBlack(byte[] out, int width, int height) {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                count += black(out, width, x, y) ? 1 : 0;
            }
        }
        return count;
    }

    @Test
    public void splitsTallImagesIntoBandsOf256Rows() {
        byte[] out = PosImageUtil.buildPosEscImageCommand(filled(10, 300, grey(255)), 10, 300, 0.7, Dithering.NONE, 0);

        assertEquals(2 + 8 + 2 * 256 + 8 + 2 * 44, out.length);
        assertArrayEquals(new byte[] {0x1B, 0x40}, Arrays.copyOfRange(out, 0, 2));
        // GS v 0, mode 0, 2 bytes per row (10 dots), 256 rows
        assertArrayEquals(new byte[] {0x1D, 0x76, 0x30, 0, 2, 0, 0, 1}, Arrays.copyOfRange(out, 2, 10));
        int second = 2 + 8 + 2 * 256;
        assertArrayEquals(new byte[] {0x1D, 0x76, 0x30, 0, 2, 0, 44, 0}, Arrays.copyOfRange(out, second, second + 8));
    }

    @Test
    public void packsTheLeftmostDotIntoTheHighBit() {
        int[] pixels = filled(16, 1, grey(255));
        pixels[0] = grey(0);
        pixels[9] = grey(0);
        byte[] out = PosImageUtil.buildPosEscImageCommand(pixels, 16, 1, 0.7, Dithering.NONE, 0);

        assertEquals((byte) 0x80, out[10]);
        assertEquals((byte) 0x40, out[11]);
    }

    @Test
    public void thresholdIsAnExclusiveLuminanceCutoff() {
        // 0.7 * 255 = 178.5, rounded half up to 179: luminance 178 prints, 179 does not.
        int[] pixels = {grey(178), grey(179)};
        byte[] out = PosImageUtil.buildPosEscImageCommand(pixels, 2, 1, 0.7, Dithering.NONE, 0);

        assertTrue(black(out, 2, 0, 0));
        assertFalse(black(out, 2, 1, 0));
    }

    @Test
    public void transparentPixelsAreWhite() {
        int[] pixels = {0x00000000, 0x7F000000, 0xFF000000};
        byte[] out = PosImageUtil.buildPosEscImageCommand(pixels, 3, 1, 0.7, Dithering.NONE, 0);

        assertFalse(black(out, 3, 0, 0));
        assertFalse(black(out, 3, 1, 0));
        assertTrue(black(out, 3, 2, 0));
    }

    @Test
    public void zeroStrengthIsTheSameAsNoDithering() {
        var random = new Random(42);
        int[] pixels = new int[64 * 64];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = grey(random.nextInt(256));
        }
        byte[] none = PosImageUtil.buildPosEscImageCommand(pixels, 64, 64, 0.7, Dithering.NONE, 1);

        assertArrayEquals(none, PosImageUtil.buildPosEscImageCommand(pixels, 64, 64, 0.7, Dithering.FLOYD_STEINBERG, 0));
        assertArrayEquals(none, PosImageUtil.buildPosEscImageCommand(pixels, 64, 64, 0.7, Dithering.ATKINSON, 0));
    }

    @Test
    public void floydSteinbergPrintsMidGreyAsAboutHalfTheDots() {
        byte[] out = PosImageUtil.buildPosEscImageCommand(
            filled(64, 64, grey(128)),
            64,
            64,
            0.7,
            Dithering.FLOYD_STEINBERG,
            1
        );

        double fraction = countBlack(out, 64, 64) / (64.0 * 64);
        assertEquals(0.5, fraction, 0.05);
    }

    @Test
    public void atkinsonKeepsDarkColoursSolidWhereFloydSteinbergSpecklesThem() {
        int[] navy = filled(64, 64, grey(40));
        byte[] atkinson = PosImageUtil.buildPosEscImageCommand(navy, 64, 64, 0.7, Dithering.ATKINSON, 1);
        byte[] floydSteinberg = PosImageUtil.buildPosEscImageCommand(navy, 64, 64, 0.7, Dithering.FLOYD_STEINBERG, 1);

        assertEquals(64 * 64, countBlack(atkinson, 64, 64));
        assertTrue(countBlack(floydSteinberg, 64, 64) < 64 * 64);
    }

    @Test
    public void ditheringKeepsLightTintsThatTheThresholdDrops() {
        // Luminance 220 is lighter than the 0.7 cutoff, so it vanishes without dithering.
        int[] tint = filled(64, 64, grey(220));

        byte[] none = PosImageUtil.buildPosEscImageCommand(tint, 64, 64, 0.7, Dithering.NONE, 0);
        assertEquals(0, countBlack(none, 64, 64));
        byte[] floydSteinberg = PosImageUtil.buildPosEscImageCommand(tint, 64, 64, 0.7, Dithering.FLOYD_STEINBERG, 1);
        assertTrue(countBlack(floydSteinberg, 64, 64) > 0);
    }
}
