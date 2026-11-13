package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FlutterAndroidUsbBulkTransferPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String TAG = "UsbBulkTransfer";

  private MethodChannel channel;
  private Context context;
  /// All USB work runs on this thread, one call at a time in the order Dart made them. bulkTransfer blocks for up to
  /// the write timeout; on the main thread that would freeze the UI and trigger "app not responding". The printer's
  /// USB permission receiver also runs here, so the printer is only ever touched from this thread.
  private HandlerThread usbThread;
  private Handler usbHandler;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private PosEscPrinter printer;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    usbThread = new HandlerThread("flutter_android_usb_bulk_transfer");
    usbThread.start();
    usbHandler = new Handler(usbThread.getLooper());
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_android_usb_bulk_transfer");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    var mainThreadResult = new MainThreadResult(result, mainHandler);
    usbHandler.post(() -> {
      try {
        handle(call, mainThreadResult);
      } catch (RuntimeException e) {
        // Same reply Flutter gives for an exception thrown on the main thread, instead of crashing the app.
        Log.e(TAG, "Failed to handle " + call.method, e);
        mainThreadResult.error("error", e.getMessage(), Log.getStackTraceString(e));
      }
    });
  }

  /// Runs on the USB thread.
  private void handle(MethodCall call, Result result) {
    switch (call.method) {
      case "listUsbDevices": {
        var usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        var devices = new ArrayList<Map<String, Object>>();
        for (var d : usbManager.getDeviceList().values()) {
          // HashMap, not Map.of: many devices report no manufacturer or product name, and Map.of rejects nulls.
          var device = new HashMap<String, Object>();
          device.put("vendorId", d.getVendorId());
          device.put("productId", d.getProductId());
          device.put("deviceId", d.getDeviceId());
          device.put("manufacturerName", d.getManufacturerName());
          device.put("productName", d.getProductName());
          device.put("deviceName", d.getDeviceName());
          device.put("deviceClass", d.getDeviceClass());
          device.put("deviceProtocol", d.getDeviceProtocol());
          devices.add(device);
        }
        result.success(devices);
        break;
      }
      case "connect": {
        Integer vid = call.argument("vid");
        Integer pid = call.argument("pid");
        if (printer != null) {
          printer.close();
        }
        printer = vid != null && pid != null
            ? new PosEscPrinter(context, usbHandler, vid, pid)
            : new PosEscPrinter(context, usbHandler);
        // Replies once the connection is established or has failed, which may be after the permission dialog.
        printer.connect(result::success);
        break;
      }
      case "write": {
        byte[] data = (byte[]) call.arguments;
        if (printer == null || !printer.isConnected()) {
          result.error("NOT_CONNECTED", "call connect() first", null);
          return;
        }
        if (!printer.write(data)) {
          result.error("WRITE_FAILED", "USB write failed; printer may be out of sync", null);
          return;
        }
        result.success(null);
        break;
      }
      case "writePdf": {
        String path = call.argument("path");
        Double threshold = call.argument("threshold");
        String ditheringName = call.argument("dithering");
        Double strength = call.argument("strength");
        if (path == null || threshold == null || ditheringName == null || strength == null) {
          result.error("INVALID_ARGUMENT", "path, threshold, dithering and strength are required", null);
          return;
        }
        PosImageUtil.Dithering dithering = switch (ditheringName) {
          case "none" -> PosImageUtil.Dithering.NONE;
          case "floydSteinberg" -> PosImageUtil.Dithering.FLOYD_STEINBERG;
          case "atkinson" -> PosImageUtil.Dithering.ATKINSON;
          default -> null;
        };
        if (dithering == null) {
          result.error("INVALID_ARGUMENT", "unknown dithering algorithm", ditheringName);
          return;
        }
        if (printer == null || !printer.isConnected()) {
          result.error("NOT_CONNECTED", "call connect() first", null);
          return;
        }
        var file = new File(path);
        if (!file.exists()) {
          result.error("FILE_NOT_FOUND", "file not found", path);
          return;
        }
        var bitmap = PdfUtil.renderPdfPage(file);
        byte[] cmd;
        try {
          cmd = PosImageUtil.buildPosEscImageCommand(bitmap, threshold, dithering, strength);
        } finally {
          bitmap.recycle();
        }
        if (!printer.write(cmd)) {
          result.error("WRITE_FAILED", "USB write failed; printer may be out of sync", path);
          return;
        }
        result.success(null);
        break;
      }
      case "read": {
        Integer length = (Integer) call.arguments;
        if (length == null || length <= 0) {
          result.error("INVALID_ARGUMENT", "length must be positive", length);
          return;
        }
        if (printer == null || !printer.isConnected()) {
          result.error("NOT_CONNECTED", "call connect() first", null);
          return;
        }
        byte[] buf = new byte[length];
        int received = printer.read(buf);
        if (received < 0) {
          result.error("READ_FAILED", "no data received: timeout, USB error, or no bulk IN endpoint", null);
          return;
        }
        result.success(Arrays.copyOf(buf, received));
        break;
      }
      case "disconnect":
        if (printer != null) {
          printer.close();
          printer = null;
        }
        result.success(null);
        break;
      case "isConnected":
        result.success(printer != null && printer.isConnected());
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    usbHandler.post(() -> {
      if (printer != null) {
        printer.close();
        printer = null;
      }
    });
    // Runs the calls already queued, including the close above, then stops the thread.
    usbThread.quitSafely();
  }

  /// Delivers replies on the main thread, where Flutter requires them, from work done on the USB thread.
  private static final class MainThreadResult implements Result {
    private final Result result;
    private final Handler mainHandler;

    MainThreadResult(Result result, Handler mainHandler) {
      this.result = result;
      this.mainHandler = mainHandler;
    }

    @Override
    public void success(@Nullable Object value) {
      mainHandler.post(() -> result.success(value));
    }

    @Override
    public void error(@NonNull String code, @Nullable String message, @Nullable Object details) {
      mainHandler.post(() -> result.error(code, message, details));
    }

    @Override
    public void notImplemented() {
      mainHandler.post(result::notImplemented);
    }
  }
}
