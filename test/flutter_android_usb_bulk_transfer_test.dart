import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_android_usb_bulk_transfer/flutter_android_usb_bulk_transfer.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('flutter_android_usb_bulk_transfer');
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  final usb = FlutterAndroidUsbBulkTransfer();
  late List<MethodCall> calls;

  setUp(() {
    calls = [];
    // Tests run with defaultTargetPlatform == android, so calls reach this stand-in for the Java side.
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return switch (call.method) {
        'connect' => true,
        'isConnected' => true,
        'read' => Uint8List.fromList([0x12]),
        'listUsbDevices' => [
          {
            'vendorId': 0x0519,
            'productId': 0x2013,
            'deviceId': 1002,
            'manufacturerName': null,
            'productName': null,
            'deviceName': '/dev/bus/usb/001/002',
            'deviceClass': 0,
            'deviceProtocol': 0,
          },
        ],
        _ => null,
      };
    });
  });

  tearDown(() {
    messenger.setMockMethodCallHandler(channel, null);
    debugDefaultTargetPlatformOverride = null;
  });

  group('writePdf', () {
    test('defaults to threshold 0.7 without dithering', () async {
      await usb.writePdf('/receipt.pdf');
      expect(calls.single.method, 'writePdf');
      expect(calls.single.arguments, {'path': '/receipt.pdf', 'threshold': 0.7, 'dithering': 'none', 'strength': 0.0});
    });

    test('sends the selected algorithm and strength', () async {
      await usb.writePdf('/receipt.pdf', threshold: 0.6, dithering: const Dithering.atkinson(strength: 0.5));
      expect(calls.single.arguments, {
        'path': '/receipt.pdf',
        'threshold': 0.6,
        'dithering': 'atkinson',
        'strength': 0.5,
      });

      calls.clear();
      await usb.writePdf('/receipt.pdf', dithering: const Dithering.floydSteinberg());
      expect(calls.single.arguments, containsPair('dithering', 'floydSteinberg'));
      expect(calls.single.arguments, containsPair('strength', 1.0));
    });

    test('rejects a threshold outside 0-1 without calling Android', () {
      expect(() => usb.writePdf('/receipt.pdf', threshold: 1.5), throwsArgumentError);
      expect(() => usb.writePdf('/receipt.pdf', threshold: -0.1), throwsArgumentError);
      expect(calls, isEmpty);
    });

    test('Dithering rejects a strength outside 0-1', () {
      expect(() => Dithering.atkinson(strength: 1.5), throwsA(isA<AssertionError>()));
      expect(() => Dithering.floydSteinberg(strength: -1), throwsA(isA<AssertionError>()));
    });
  });

  test('listUsbDevices keeps devices that report no manufacturer or product name', () async {
    final devices = await usb.listUsbDevices();
    expect(devices.single['vendorId'], 0x0519);
    expect(devices.single['manufacturerName'], isNull);
    expect(devices.single['productName'], isNull);
  });

  test('connect sends the vendor and product IDs', () async {
    expect(await usb.connect(vid: 0x0519, pid: 0x2013), isTrue);
    expect(calls.single.arguments, {'vid': 0x0519, 'pid': 0x2013});
  });

  test('read returns the received bytes and rejects a non-positive length', () async {
    expect(await usb.read(8), [0x12]);
    expect(() => usb.read(0), throwsArgumentError);
    expect(calls.length, 1);
  });

  test('Android errors reach the caller as PlatformException', () async {
    messenger.setMockMethodCallHandler(channel, (call) async {
      throw PlatformException(code: 'NOT_CONNECTED', message: 'call connect() first');
    });
    await expectLater(
      usb.write(Uint8List.fromList([0x1B, 0x40])),
      throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'NOT_CONNECTED')),
    );
  });

  test('does nothing on other platforms', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    expect(await usb.listUsbDevices(), isEmpty);
    expect(await usb.connect(vid: 1, pid: 2), isFalse);
    expect(await usb.read(4), isNull);
    expect(await usb.isConnected(), isFalse);
    await usb.writePdf('/receipt.pdf');
    expect(calls, isEmpty);
  });
}
