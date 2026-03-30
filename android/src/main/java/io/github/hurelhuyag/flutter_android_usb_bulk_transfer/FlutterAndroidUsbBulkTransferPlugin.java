package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.content.Context;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.io.File;

/** FlutterAndroidUsbBulkTransferPlugin */
public class FlutterAndroidUsbBulkTransferPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Context context;
  private PosEscPrinter printer;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_android_usb_bulk_transfer");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "connect":
        Integer vid = call.argument("vid");
        Integer pid = call.argument("pid");
        if (vid != null && pid != null) {
          printer = new PosEscPrinter(context, vid, pid);
        } else {
          printer = new PosEscPrinter(context);
        }
        result.success(null);
        break;
      case "write":
        byte[] data = (byte[]) call.arguments;
        if (printer != null) {
          printer.write(data);
        }
        result.success(null);
        break;
      case "writePdf":
        String path = (String) call.arguments;
        if (printer != null) {
          var file = new File(path);
          if (!file.exists()) {
            result.error("FILE_NOT_FOUND", "file not found", path);
            return;
          }
          var bitmap = PdfUtil.renderPdfPage(file);
          var cmd = PosImageUtil.buildPosEscImageCommand(bitmap);
          printer.write(cmd);
        }
        result.success(null);
        break;
      case "read":
        Integer length = (Integer) call.arguments;
        if (printer != null && length != null) {
          byte[] buf = new byte[length];
          printer.read(buf);
          result.success(buf);
        } else {
          result.success(null);
        }
        break;
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
    if (printer != null) {
      printer.close();
      printer = null;
    }
    context = null;
  }
}
