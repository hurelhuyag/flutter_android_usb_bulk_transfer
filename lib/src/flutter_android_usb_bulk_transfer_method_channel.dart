import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_android_usb_bulk_transfer_platform_interface.dart';

/// An implementation of [FlutterAndroidUsbBulkTransferPlatform] that uses method channels.
class MethodChannelFlutterAndroidUsbBulkTransfer extends FlutterAndroidUsbBulkTransferPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_android_usb_bulk_transfer');

  @override
  Future<List<Map<String, dynamic>>> listUsbDevices() async {
    if (!Platform.isAndroid) return [];
    final devices = await methodChannel.invokeListMethod<Map<dynamic, dynamic>>('listUsbDevices');
    return devices?.map((e) => e.cast<String, dynamic>()).toList() ?? [];
  }
  
  @override
  Future<void> connect({int? vid, int? pid}) async {
    if (!Platform.isAndroid) return;
    await methodChannel.invokeMethod('connect', {
      'vid': vid,
      'pid': pid,
    });
  }

  @override
  Future<void> write(Uint8List data) async {
    if (!Platform.isAndroid) return;
    await methodChannel.invokeMethod('write', data);
  }

  @override
  Future<void> writePdf(String path) async {
    if (!Platform.isAndroid) return;
    await methodChannel.invokeMethod('writePdf', path);
  }

  @override
  Future<Uint8List?> read(int length) async {
    if (!Platform.isAndroid) return null;
    return await methodChannel.invokeMethod<Uint8List>('read', length);
  }

  @override
  Future<void> disconnect() async {
    if (!Platform.isAndroid) return;
    await methodChannel.invokeMethod('disconnect');
  }

  @override
  Future<bool> isConnected() async {
    if (!Platform.isAndroid) return false;
    return await methodChannel.invokeMethod('isConnected');
  }
}
