import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/services/bluetooth_service.dart';
import '../../domain/repositories/bluetooth_repository.dart';

final bluetoothRepositoryProvider = Provider<BluetoothRepository>((ref) {
  return BluetoothServiceImpl();
});

final scanResultsProvider = StreamProvider<List<ScanResult>>((ref) {
  final bluetoothRepository = ref.watch(bluetoothRepositoryProvider);
  return bluetoothRepository.scanResults;
});

final isScanningProvider = StreamProvider<bool>((ref) {
  final bluetoothRepository = ref.watch(bluetoothRepositoryProvider);
  return bluetoothRepository.isScanning;
});

final connectedDeviceProvider = StreamProvider<BluetoothDevice?>((ref) {
  final bluetoothRepository = ref.watch(bluetoothRepositoryProvider);
  return bluetoothRepository.connectedDevice;
});
