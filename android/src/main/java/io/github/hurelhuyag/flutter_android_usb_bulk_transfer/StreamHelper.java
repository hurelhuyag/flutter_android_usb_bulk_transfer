package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamHelper {

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192]; // 8 KB buffer (good default)
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }
}
