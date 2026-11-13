package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class PosImageUtil {

    /// Rows per GS v 0 command. Printers cap the height of a single raster command (Epson TM: 2303 dots, many
    /// clones far less); exceeding it makes the printer reject the header and print the bitmap bytes as text.
    private static final int BAND_HEIGHT = 256;

    /// Widest horizontal reach of any kernel below, used to pad the error rows so edges need no bounds checks.
    private static final int PAD = 2;

    /// Error-diffusion kernels: each entry pushes {@code weight} of a pixel's rounding error to the pixel at
    /// ({@code dx}, {@code dy}) relative to it, with dx mirrored on right-to-left rows.
    public enum Dithering {
        /// Solid black and white, decided by the threshold alone.
        NONE(new int[0], new int[0], new float[0]),
        /// Floyd-Steinberg (1976): all of the error to 4 neighbours. Smooth, fine-grained greys.
        FLOYD_STEINBERG(
            new int[] {1, -1, 0, 1},
            new int[] {0, 1, 1, 1},
            new float[] {7f / 16, 3f / 16, 5f / 16, 1f / 16}
        ),
        /// Atkinson (Apple, 1980s): 6/8 of the error to 6 neighbours over two rows. Higher contrast; dark colours
        /// stay solid black and light tints print as sparser dots.
        ATKINSON(
            new int[] {1, 2, -1, 0, 1, 0},
            new int[] {0, 0, 1, 1, 1, 2},
            new float[] {1f / 8, 1f / 8, 1f / 8, 1f / 8, 1f / 8, 1f / 8}
        );

        private final int[] dx;
        private final int[] dy;
        private final float[] weight;

        Dithering(int[] dx, int[] dy, float[] weight) {
            this.dx = dx;
            this.dy = dy;
            this.weight = weight;
        }
    }

    public static byte[] buildPosEscImageCommand(Bitmap bm, double threshold, Dithering dithering, double strength) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int[] pixels = new int[width * height];
        bm.getPixels(pixels, 0, width, 0, 0, width, height);
        return buildPosEscImageCommand(pixels, width, height, threshold, dithering, strength);
    }

    /// Converts ARGB pixels to an ESC/POS raster image.
    ///
    /// @param threshold luminance (0-1) below which a pixel prints black. 0.7 keeps logos with light gradients whole
    ///                  and white text readable on grey or coloured fills; QR codes and black text are unaffected.
    /// @param dithering error-diffusion algorithm used to simulate greys; {@link Dithering#NONE} prints solid black
    ///                  and white. Rows are scanned in alternating directions (serpentine) to avoid streaks.
    /// @param strength  fraction (0-1) of the algorithm's error that is spread; 1 is the standard algorithm, lower
    ///                  values keep more of the image solid.
    public static byte[] buildPosEscImageCommand(
        int[] pixels,
        int width,
        int height,
        double threshold,
        Dithering dithering,
        double strength
    ) {
        int cut = (int) Math.round(threshold * 255);
        float s = (float) strength;
        boolean diffuse = dithering != Dithering.NONE && s > 0;
        int bytesPerRow = (width + 7) / 8;
        int bands = (height + BAND_HEIGHT - 1) / BAND_HEIGHT;
        var out = new ByteArrayOutputStream(2 + bands * 8 + bytesPerRow * height);
        // Diffused error for the current row and the two below it (Atkinson reaches two rows down).
        var err = new float[3][width + 2 * PAD];
        var row = new byte[bytesPerRow];

        // ESC @ - reset printer state left over from previous jobs
        out.write(0x1B);
        out.write(0x40);
        for (int top = 0; top < height; top += BAND_HEIGHT) {
            int rows = Math.min(BAND_HEIGHT, height - top);
            // GS v 0 m xL xH yL yH
            byte[] header = {
                0x1D, 0x76, 0x30, 0x00,
                (byte) bytesPerRow, (byte) (bytesPerRow >> 8), (byte) rows, (byte) (rows >> 8)
            };
            out.write(header, 0, header.length);
            for (int y = top; y < top + rows; y++) {
                Arrays.fill(row, (byte) 0);
                int dir = (y & 1) == 0 ? 1 : -1;
                for (int k = 0; k < width; k++) {
                    int x = dir == 1 ? k : width - 1 - k;
                    int argb = pixels[y * width + x];
                    // Transparent pixels count as paper white.
                    int lum = (argb >>> 24) < 128
                        ? 255
                        : (((argb >> 16) & 0xFF) * 299 + ((argb >> 8) & 0xFF) * 587 + (argb & 0xFF) * 114) / 1000;
                    float v = lum + err[0][x + PAD];
                    boolean black = v < cut;
                    if (black) {
                        row[x >> 3] |= (byte) (0x80 >> (x & 7));
                    }
                    if (diffuse) {
                        float e = (v - (black ? 0 : 255)) * s;
                        for (int i = 0; i < dithering.weight.length; i++) {
                            err[dithering.dy[i]][x + PAD + dithering.dx[i] * dir] += e * dithering.weight[i];
                        }
                    }
                }
                out.write(row, 0, row.length);
                var done = err[0];
                err[0] = err[1];
                err[1] = err[2];
                Arrays.fill(done, 0);
                err[2] = done;
            }
        }
        return out.toByteArray();
    }
}
