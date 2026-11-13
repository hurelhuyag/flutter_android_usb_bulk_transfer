/// USB bulk transfers on Android, with ESC/POS printing of PDF pages.
library;

import 'dart:typed_data';

import 'src/dithering.dart';
import 'src/flutter_android_usb_bulk_transfer_platform_interface.dart';

export 'src/dithering.dart';
export 'src/flutter_android_usb_bulk_transfer_platform_interface.dart' show FlutterAndroidUsbBulkTransferPlatform;

/// Talks to one USB device at a time through its bulk endpoints.
///
/// Every method does nothing on platforms other than Android: it completes with an empty list, `false` or `null`.
/// Calls run on a background thread on Android, one at a time in the order they were made, so a slow device never
/// blocks the UI.
///
/// Failures are reported as a `PlatformException` with one of these codes:
/// - `NOT_CONNECTED`: [write], [writePdf] or [read] was called before a successful [connect].
/// - `WRITE_FAILED`: not all data reached the device. A printer may be left in the middle of a command; power-cycle it.
/// - `READ_FAILED`: nothing arrived within 5 seconds, the transfer failed, or the device has no bulk IN endpoint.
/// - `FILE_NOT_FOUND`: the PDF passed to [writePdf] does not exist.
/// - `INVALID_ARGUMENT`: an argument was rejected on the Android side.
class FlutterAndroidUsbBulkTransfer {
  /// Lists the USB devices attached to the phone or tablet.
  ///
  /// Each map has the keys `vendorId`, `productId`, `deviceId`, `deviceName`, `deviceClass` and `deviceProtocol`
  /// (all present), plus `manufacturerName` and `productName`, which are `null` when the device does not report them.
  Future<List<Map<String, dynamic>>> listUsbDevices() {
    return FlutterAndroidUsbBulkTransferPlatform.instance.listUsbDevices();
  }

  /// Connects to the USB device with the given vendor/product ID.
  ///
  /// When USB permission hasn't been granted yet, Android shows a permission dialog and the returned future
  /// completes only after the user answers it. Completes with `true` when connected, `false` when the device was not
  /// found, permission was denied, or the device could not be opened. Connecting again closes the previous connection.
  Future<bool> connect({int? vid, int? pid}) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.connect(vid: vid, pid: pid);
  }

  /// Sends [data] to the device's bulk OUT endpoint, in 4 KB chunks.
  ///
  /// Each chunk waits up to 30 seconds for the device, which covers a printer that is busy printing a large image.
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

  /// Reads up to [length] bytes from the device's bulk IN endpoint, waiting up to 5 seconds.
  ///
  /// Completes with only the bytes that arrived, which may be fewer than [length]. Completes with `null` on
  /// platforms other than Android.
  Future<Uint8List?> read(int length) {
    if (length <= 0) throw ArgumentError.value(length, 'length', 'must be positive');
    return FlutterAndroidUsbBulkTransferPlatform.instance.read(length);
  }

  /// Releases the device. Safe to call when not connected.
  Future<void> disconnect() {
    return FlutterAndroidUsbBulkTransferPlatform.instance.disconnect();
  }

  /// Whether a [connect] call succeeded and [disconnect] has not been called since.
  Future<bool> isConnected() {
    return FlutterAndroidUsbBulkTransferPlatform.instance.isConnected();
  }
}
