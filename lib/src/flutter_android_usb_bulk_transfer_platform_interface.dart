import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'dithering.dart';
import 'flutter_android_usb_bulk_transfer_method_channel.dart';

/// The interface that implementations of this plugin must extend.
///
/// Apps use `FlutterAndroidUsbBulkTransfer` instead; it validates arguments and delegates to [instance]. Tests can
/// replace [instance] with a fake that extends this class.
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

  /// Lists attached USB devices. See `FlutterAndroidUsbBulkTransfer.listUsbDevices`.
  Future<List<Map<String, dynamic>>> listUsbDevices() {
    throw UnimplementedError('listUsbDevices() has not been implemented.');
  }

  /// Connects to a USB device. See `FlutterAndroidUsbBulkTransfer.connect`.
  Future<bool> connect({int? vid, int? pid}) {
    throw UnimplementedError('connect() has not been implemented.');
  }

  /// Sends bytes to the bulk OUT endpoint. See `FlutterAndroidUsbBulkTransfer.write`.
  Future<void> write(Uint8List data) {
    throw UnimplementedError('write() has not been implemented.');
  }

  /// Prints a PDF page as an ESC/POS image. See `FlutterAndroidUsbBulkTransfer.writePdf`.
  Future<void> writePdf(String path, {required double threshold, required Dithering dithering}) {
    throw UnimplementedError('writePdf() has not been implemented.');
  }

  /// Reads bytes from the bulk IN endpoint. See `FlutterAndroidUsbBulkTransfer.read`.
  Future<Uint8List?> read(int length) {
    throw UnimplementedError('read() has not been implemented.');
  }

  /// Releases the device. See `FlutterAndroidUsbBulkTransfer.disconnect`.
  Future<void> disconnect() {
    throw UnimplementedError('disconnect() has not been implemented.');
  }

  /// Whether a device is connected. See `FlutterAndroidUsbBulkTransfer.isConnected`.
  Future<bool> isConnected() {
    throw UnimplementedError('isConnected() has not been implemented.');
  }
}
