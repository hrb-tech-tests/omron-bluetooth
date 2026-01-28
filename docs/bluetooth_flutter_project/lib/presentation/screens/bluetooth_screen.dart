import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import '../providers/bluetooth_providers.dart';

class BluetoothScreen extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scanResults = ref.watch(scanResultsProvider);
    final isScanning = ref.watch(isScanningProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text('Bluetooth Scanner'),
      ),
      body: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              ElevatedButton(
                onPressed: () {
                  ref.read(bluetoothRepositoryProvider).startScan();
                },
                child: Text('Scan'),
              ),
              ElevatedButton(
                onPressed: () {
                  ref.read(bluetoothRepositoryProvider).stopScan();
                },
                child: Text('Stop'),
              ),
            ],
          ),
          isScanning.when(
            data: (scanning) => scanning ? LinearProgressIndicator() : Container(),
            loading: () => LinearProgressIndicator(),
            error: (err, stack) => Text('Error: $err'),
          ),
          Expanded(
            child: scanResults.when(
              data: (results) {
                return ListView.builder(
                  itemCount: results.length,
                  itemBuilder: (context, index) {
                    final result = results[index];
                    return ListTile(
                      title: Text(result.device.name.isNotEmpty ? result.device.name : 'Unknown Device'),
                      subtitle: Text(result.device.id.id),
                      trailing: ElevatedButton(
                        onPressed: () {
                          ref.read(bluetoothRepositoryProvider).connect(result.device);
                        },
                        child: Text('Connect'),
                      ),
                    );
                  },
                );
              },
              loading: () => Center(child: CircularProgressIndicator()),
              error: (err, stack) => Center(child: Text('Error: $err')),
            ),
          ),
        ],
      ),
    );
  }
}
