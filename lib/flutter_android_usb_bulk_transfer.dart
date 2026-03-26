import 'dart:typed_data';

import 'src/flutter_android_usb_bulk_transfer_platform_interface.dart';

export 'src/flutter_android_usb_bulk_transfer_platform_interface.dart' show FlutterAndroidUsbBulkTransferPlatform;

class FlutterAndroidUsbBulkTransfer {

  Future<void> connect({int? vid, int? pid}) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.connect(vid: vid, pid: pid);
  }

  Future<void> write(Uint8List data) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.write(data);
  }

  Future<Uint8List?> read(int length) {
    return FlutterAndroidUsbBulkTransferPlatform.instance.read(length);
  }
}
