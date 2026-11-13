import 'dart:typed_data';

import 'src/dithering.dart';
import 'src/flutter_android_usb_bulk_transfer_platform_interface.dart';

export 'src/dithering.dart';
export 'src/flutter_android_usb_bulk_transfer_platform_interface.dart' show FlutterAndroidUsbBulkTransferPlatform;

class FlutterAndroidUsbBulkTransfer {

  Future<List<Map<String, dynamic>>> listUsbDevices() {
    return FlutterAndroidUsbBulkTransferPlatform.instance.listUsbDevices();
  }
  
  /// Connects to the USB device with the given vendor/product ID.
  ///
  /// When USB permission hasn't been granted yet, Android shows a permission dialog and the returned future
  /// completes only after the user answers it. Completes with `true` when connected, `false` when the device was not
  /// found, permission was denied, or the device could not be opened.
  Future<bool> connect({int? vid, int? pid}) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.connect(vid: vid, pid: pid);
  }

  Future<void> write(Uint8List data) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.write(data);
  }

  /// Prints the first page of the PDF at [path] as an ESC/POS raster image, 576 dots wide.
  ///
  /// [threshold] (0-1) is the luminance below which a dot prints black. Raise it to print darker. The default 0.7
  /// keeps logos with light gradients whole and white text readable on grey or coloured backgrounds.
  ///
  /// [dithering] chooses how greys and colours are simulated with dot patterns. The default [Dithering.none] prints
  /// solid black and white, which is best for logos, QR codes and text. [Dithering.floydSteinberg] and
  /// [Dithering.atkinson] reproduce gradients, shadows and photos.
  Future<void> writePdf(String path, {double threshold = 0.7, Dithering dithering = Dithering.none}) {
    if (threshold < 0 || threshold > 1) throw ArgumentError.value(threshold, 'threshold', 'must be between 0 and 1');
    if (dithering.strength < 0 || dithering.strength > 1) {
      throw ArgumentError.value(dithering.strength, 'dithering.strength', 'must be between 0 and 1');
    }
    return FlutterAndroidUsbBulkTransferPlatform.instance.writePdf(path, threshold: threshold, dithering: dithering);
  }

  Future<Uint8List?> read(int length) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.read(length);
  }

  Future<void> disconnect() {
    return FlutterAndroidUsbBulkTransferPlatform.instance.disconnect();
  }

  Future<bool> isConnected() {
    return FlutterAndroidUsbBulkTransferPlatform.instance.isConnected();
  }
}
