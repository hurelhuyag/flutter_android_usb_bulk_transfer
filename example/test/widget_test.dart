import 'package:flutter/services.dart';
import 'package:flutter_android_usb_bulk_transfer_example/main.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

void main() {
  const channel = MethodChannel('flutter_android_usb_bulk_transfer');

  setUp(() {
    // Stand in for the Android side: no USB devices attached.
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (call) async => call.method == 'listUsbDevices' ? <Map<String, dynamic>>[] : null,
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  testWidgets('shows an empty device list and keeps printing disabled until connected', (tester) async {
    await tester.pumpWidget(const ExampleApp());
    await tester.pumpAndSettle();

    expect(find.text('No USB devices attached. Plug in a printer and tap refresh.'), findsOneWidget);
    expect(find.text('List devices: 0 found'), findsOneWidget);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Print receipt')).onPressed, isNull);
  });
}
