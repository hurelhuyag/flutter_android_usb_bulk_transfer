# flutter_android_usb_bulk_transfer

A Flutter plugin for Android USB bulk transfer. It allows communicating with USB devices using bulk endpoints.

## Features

- List connected USB devices.
- Connect to a USB device by Vendor ID and Product ID.
- Write raw bytes to a USB device.
- Read bytes from a USB device.
- Check connection status and disconnect.
- **Platform Safety**: Does nothing on non-Android platforms instead of failing.

## Usage

### List USB Devices

```dart
final plugin = FlutterAndroidUsbBulkTransfer();
List<Map<String, dynamic>> devices = await plugin.listUsbDevices();
for (var device in devices) {
  print('Device: ${device['deviceName']}, VID: ${device['vendorId']}, PID: ${device['productId']}');
}
```

### Connect to a USB Device

```dart
// Connect using specific VID and PID
await plugin.connect(vid: 1234, pid: 5678);
```

### Write Data

```dart
final data = Uint8List.fromList([0x01, 0x02, 0x03, 0x04]);
await plugin.write(data);
```

### Read Data

```dart
final response = await plugin.read(64); // Read 64 bytes
if (response != null) {
  print('Received: $response');
}
```

### Check Connection and Disconnect

```dart
bool connected = await plugin.isConnected();
if (connected) {
  await plugin.disconnect();
}
```

## Bonus: POS/ESC Printer Support

This plugin includes specialized functionality for POS/ESC printers.

### Print PDF Page

Renders a PDF page into a bitmap and converts it to ESC/POS image commands.

```dart
await plugin.writePdf('/path/to/your/document.pdf');
```

## Android Setup

Ensure your `AndroidManifest.xml` includes the USB host feature. The plugin handles runtime permission requests.

```xml
<uses-feature android:name="android.hardware.usb.host" />
```

## License

Apache 2.0
