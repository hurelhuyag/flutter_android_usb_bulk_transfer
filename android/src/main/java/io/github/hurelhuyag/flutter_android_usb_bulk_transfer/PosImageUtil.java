package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;

public class PosImageUtil {

    /// Rows per GS v 0 command. Printers cap the height of a single raster command (Epson TM: 2303 dots, many
    /// clones far less); exceeding it makes the printer reject the header and print the bitmap bytes as text.
    private static final int BAND_HEIGHT = 256;

    public static byte[] buildPosEscImageCommand(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int[] pixels = new int[width * height];
        bm.getPixels(pixels, 0, width, 0, 0, width, height);

        int bytesPerRow = (width + 7) / 8;
        int bands = (height + BAND_HEIGHT - 1) / BAND_HEIGHT;
        var out = new ByteArrayOutputStream(2 + bands * 8 + bytesPerRow * height);
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
                for (int xb = 0; xb < bytesPerRow; xb++) {
                    int b = 0;
                    for (int bit = 0; bit < 8; bit++) {
                        int x = xb * 8 + bit;
                        if (x >= width) {
                            break;
                        }
                        int argb = pixels[y * width + x];
                        int lum = (Color.red(argb) * 299 + Color.green(argb) * 587 + Color.blue(argb) * 114) / 1000;
                        if (Color.alpha(argb) >= 128 && lum < 128) {
                            b |= 0x80 >> bit;
                        }
                    }
                    out.write(b);
                }
            }
        }
        return out.toByteArray();
    }
}
