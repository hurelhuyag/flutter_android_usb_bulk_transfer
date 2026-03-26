import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_android_usb_bulk_transfer_method_channel.dart';

abstract class FlutterAndroidUsbBulkTransferPlatform extends PlatformInterface {
  /// Constructs a FlutterAndroidUsbBulkTransferPlatform.
  FlutterAndroidUsbBulkTransferPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterAndroidUsbBulkTransferPlatform _instance = MethodChannelFlutterAndroidUsbBulkTransfer();

  /// The default instance of [FlutterAndroidUsbBulkTransferPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterAndroidUsbBulkTransfer].
  static FlutterAndroidUsbBulkTransferPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterAndroidUsbBulkTransferPlatform] when
  /// they register themselves.
  static set instance(FlutterAndroidUsbBulkTransferPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> connect({int? vid, int? pid}) {
    throw UnimplementedError('connect() has not been implemented.');
  }

  Future<void> write(Uint8List data) {
    throw UnimplementedError('write() has not been implemented.');
  }

  Future<Uint8List?> read(int length) {
    throw UnimplementedError('read() has not been implemented.');
  }

  Future<void> disconnect() {
    throw UnimplementedError('connect() has not been implemented.');
  }

  Future<bool> isConnected() {
    throw UnimplementedError('connect() has not been implemented.');
  }
}
