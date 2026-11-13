import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'dithering.dart';
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
  Future<bool> connect({int? vid, int? pid}) async {
    if (!Platform.isAndroid) return false;
    final connected = await methodChannel.invokeMethod<bool>('connect', {
      'vid': vid,
      'pid': pid,
    });
    return connected ?? false;
  }

  @override
  Future<void> write(Uint8List data) async {
    if (!Platform.isAndroid) return;
    await methodChannel.invokeMethod('write', data);
  }

  @override
  Future<void> writePdf(String path, {required double threshold, required Dithering dithering}) async {
    if (!Platform.isAndroid) return;
    await methodChannel.invokeMethod('writePdf', {
      'path': path,
      'threshold': threshold,
      'dithering': dithering.algorithm.name,
      'strength': dithering.strength,
    });
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
