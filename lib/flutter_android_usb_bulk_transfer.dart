import 'dart:typed_data';

import 'src/flutter_android_usb_bulk_transfer_platform_interface.dart';

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

  Future<void> writePdf(String path) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.writePdf(path);
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
