# flutter_android_usb_bulk_transfer example

Connects to a USB ESC/POS printer and prints a sample receipt.

1. Connect the printer to an Android phone or tablet with a USB OTG cable.
2. Run the app with `flutter run`.
3. Tap the printer in the device list and allow USB access when Android asks.
4. Choose a threshold and dithering algorithm, then tap **Print receipt**.

**Print text** sends raw ESC/POS commands, and **Read status** asks the printer for its status byte. Printers that
don't answer status requests show `READ_FAILED`.
