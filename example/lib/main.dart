import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter_android_usb_bulk_transfer/flutter_android_usb_bulk_transfer.dart';
import 'package:material_ui/material_ui.dart';
import 'package:path_provider/path_provider.dart';

void main() {
  runApp(const ExampleApp());
}

class ExampleApp extends StatelessWidget {
  const ExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'USB Printer Example',
      theme: ThemeData(colorScheme: .fromSeed(seedColor: Colors.indigo)),
      home: const PrinterPage(),
    );
  }
}

class PrinterPage extends StatefulWidget {
  const PrinterPage({super.key});

  @override
  State<PrinterPage> createState() => _PrinterPageState();
}

class _PrinterPageState extends State<PrinterPage> {
  final _usb = FlutterAndroidUsbBulkTransfer();
  final _log = <String>[];
  List<Map<String, dynamic>> _devices = [];
  Map<String, dynamic>? _connected;
  bool _busy = false;
  double _threshold = 0.7;
  DitheringAlgorithm _algorithm = DitheringAlgorithm.none;

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  /// Runs [action], shows its outcome in the log, and disables the buttons meanwhile.
  Future<void> _run(String label, Future<String> Function() action) async {
    setState(() => _busy = true);
    String line;
    try {
      line = '$label: ${await action()}';
    } on PlatformException catch (e) {
      line = '$label failed: ${e.code} ${e.message ?? ''}';
    } catch (e) {
      line = '$label failed: $e';
    }
    if (!mounted) return;
    setState(() {
      _log.insert(0, line);
      _busy = false;
    });
  }

  Future<void> _refresh() => _run('List devices', () async {
    final devices = await _usb.listUsbDevices();
    setState(() => _devices = devices);
    return '${devices.length} found';
  });

  Future<void> _connect(Map<String, dynamic> device) => _run('Connect', () async {
    // Shows Android's USB permission dialog the first time, and completes after the user answers it.
    final ok = await _usb.connect(vid: device['vendorId'], pid: device['productId']);
    setState(() => _connected = ok ? device : null);
    return ok ? 'connected' : 'not connected (device missing or permission denied)';
  });

  Future<void> _printReceipt() => _run('Print receipt', () async {
    // writePdf needs a file path, so copy the bundled PDF out of the app's assets first.
    final pdf = await rootBundle.load('assets/sample_receipt.pdf');
    final file = File('${(await getTemporaryDirectory()).path}/sample_receipt.pdf');
    await file.writeAsBytes(pdf.buffer.asUint8List(pdf.offsetInBytes, pdf.lengthInBytes));
    final dithering = switch (_algorithm) {
      DitheringAlgorithm.none => Dithering.none,
      DitheringAlgorithm.floydSteinberg => const Dithering.floydSteinberg(),
      DitheringAlgorithm.atkinson => const Dithering.atkinson(),
    };
    await _usb.writePdf(file.path, threshold: _threshold, dithering: dithering);
    return 'sent';
  });

  Future<void> _printText() => _run('Print text', () async {
    final bytes = BytesBuilder()
      ..add([0x1B, 0x40]) // ESC @: reset
      ..add([0x1B, 0x61, 0x01]) // ESC a 1: centre
      ..add(ascii.encode('Hello from Flutter!\n\n\n'))
      ..add([0x1D, 0x56, 0x01]); // GS V 1: partial cut
    await _usb.write(bytes.toBytes());
    return 'sent ${bytes.length} bytes';
  });

  Future<void> _readStatus() => _run('Printer status', () async {
    // DLE EOT 1: ask an ESC/POS printer for its status byte.
    await _usb.write(Uint8List.fromList([0x10, 0x04, 0x01]));
    final reply = await _usb.read(1);
    return reply == null || reply.isEmpty ? 'no reply' : '0x${reply.first.toRadixString(16).padLeft(2, '0')}';
  });

  Future<void> _disconnect() => _run('Disconnect', () async {
    await _usb.disconnect();
    setState(() => _connected = null);
    return 'done';
  });

  @override
  Widget build(BuildContext context) {
    final ready = _connected != null && !_busy;
    return Scaffold(
      appBar: AppBar(
        title: const Text('USB Printer Example'),
        actions: [IconButton(onPressed: _busy ? null : _refresh, icon: const Icon(Icons.refresh))],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('USB devices', style: Theme.of(context).textTheme.titleMedium),
          if (_devices.isEmpty) const Text('No USB devices attached. Plug in a printer and tap refresh.'),
          for (final d in _devices)
            ListTile(
              title: Text(d['productName'] ?? 'Unnamed device'),
              subtitle: Text(
                '${d['manufacturerName'] ?? 'Unknown maker'} · '
                '${d['vendorId'].toRadixString(16)}:${d['productId'].toRadixString(16)}',
              ),
              trailing: _connected?['deviceId'] == d['deviceId'] ? const Icon(Icons.check_circle) : null,
              onTap: _busy ? null : () => _connect(d),
            ),
          const Divider(),
          Text('Image conversion', style: Theme.of(context).textTheme.titleMedium),
          Row(
            children: [
              const Text('Threshold'),
              Expanded(
                child: Slider(
                  value: _threshold,
                  min: 0.4,
                  max: 0.9,
                  divisions: 10,
                  label: _threshold.toStringAsFixed(2),
                  onChanged: (v) => setState(() => _threshold = v),
                ),
              ),
              Text(_threshold.toStringAsFixed(2)),
            ],
          ),
          SegmentedButton<DitheringAlgorithm>(
            segments: const [
              ButtonSegment(value: DitheringAlgorithm.none, label: Text('None')),
              ButtonSegment(value: DitheringAlgorithm.floydSteinberg, label: Text('Floyd–Steinberg')),
              ButtonSegment(value: DitheringAlgorithm.atkinson, label: Text('Atkinson')),
            ],
            selected: {_algorithm},
            onSelectionChanged: (s) => setState(() => _algorithm = s.first),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              FilledButton(onPressed: ready ? _printReceipt : null, child: const Text('Print receipt')),
              FilledButton.tonal(onPressed: ready ? _printText : null, child: const Text('Print text')),
              OutlinedButton(onPressed: ready ? _readStatus : null, child: const Text('Read status')),
              OutlinedButton(onPressed: ready ? _disconnect : null, child: const Text('Disconnect')),
            ],
          ),
          if (_busy) const Padding(padding: EdgeInsets.only(top: 16), child: LinearProgressIndicator()),
          const Divider(),
          for (final line in _log) Text(line),
        ],
      ),
    );
  }
}
