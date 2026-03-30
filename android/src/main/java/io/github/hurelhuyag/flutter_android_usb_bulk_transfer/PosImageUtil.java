package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.graphics.Bitmap;

public class PosImageUtil {

    public static byte[] buildPosEscImageCommand(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int[] pixels = new int[width * height];
        bm.getPixels(pixels, 0, width, 0, 0, width, height);
        return buildPosEscImageCommand(pixels, width, height);
    }

    private static byte[] buildPosEscImageCommand(int[] pixels, int width, int height) {
        byte[] var4;
        if ((width = (var4 = getBMPImageFileByte(pixels, width, height)).length) == 0) {
            return null;
        } else {
            byte[] buf = new byte[width];

            System.arraycopy(var4, 0, buf, 0, width);

            return buf;
        }
    }

    //  FROM MSPrintDemo project
    //  convert bmp byte to print byte
    private static byte[] getBMPImageFileByte(int[] pixels, int width, int height) {
        int p = width / 8;
        if ((width %= 8) > 0) {
            ++p;
        }

        byte[] result = new byte[p * height + 8];
        byte m = 0;
        int l = m + 1;
        result[0] = 29;
        ++l;
        result[1] = 118;
        ++l;
        result[2] = 48;
        ++l;
        result[3] = 0;
        ++l;
        result[4] = (byte)p;
        ++l;
        result[5] = (byte)(p >> 8);
        ++l;
        result[6] = (byte)height;
        ++l;
        result[7] = (byte)(height >> 8);
        int n = 0;

        for(int k = 0; k < height; ++k) {
            int i;
            int j;
            for(j = 0; j < p - 1; ++j) {
                i = 0;
                if (pixels[n++] < -1) {
                    i += 128;
                }

                if (pixels[n++] < -1) {
                    i += 64;
                }

                if (pixels[n++] < -1) {
                    i += 32;
                }

                if (pixels[n++] < -1) {
                    i += 16;
                }

                if (pixels[n++] < -1) {
                    i += 8;
                }

                if (pixels[n++] < -1) {
                    i += 4;
                }

                if (pixels[n++] < -1) {
                    i += 2;
                }

                if (pixels[n++] < -1) {
                    ++i;
                }

                result[l++] = (byte)i;
            }

            i = 0;
            if (width == 0) {
                for(j = 8; j > width; --j) {
                    if (pixels[n++] < -1) {
                        i += 1 << j;
                    }
                }
            } else {
                for(j = 0; j < width; ++j) {
                    if (pixels[n++] < -1) {
                        i += 1 << 8 - j;
                    }
                }
            }

            result[l++] = (byte)i;
        }

        return result;
    }
}
