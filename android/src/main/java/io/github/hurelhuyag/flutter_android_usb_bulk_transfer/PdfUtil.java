package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class PdfUtil {

    public static Bitmap renderPdfPage(File pdfFile) {
        try (var fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
             var pdfRenderer = new PdfRenderer(fileDescriptor);
             var page = pdfRenderer.openPage(0)) {
            var pdfW = page.getWidth();
            var pdfH = page.getHeight();
            Log.d("PdfUtil", "page width:" + pdfW + ", height: " + pdfH);

            var imageW = 576;
            var imageH = (imageW * pdfH) / pdfW;
            Log.d("PdfUtil", "image width:" + imageW + ", height: " + imageH);

            var bitmap = Bitmap.createBitmap(imageW, imageH, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } catch (IOException e ){
            throw new RuntimeException(e);
        }
    }
}
