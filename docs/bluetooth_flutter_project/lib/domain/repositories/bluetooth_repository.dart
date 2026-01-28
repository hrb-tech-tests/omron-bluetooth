import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import '../entities/omron_measurement.dart';

abstract class BluetoothRepository {
  Stream<List<ScanResult>> get scanResults;
  Stream<bool> get isScanning;
  Stream<BluetoothDevice?> get connectedDevice;

  Future<void> startScan();
  Future<void> stopScan();
  Future<void> connect(BluetoothDevice device);
  Future<void> disconnect();
  Stream<List<OmronMeasurement>> subscribeToMeasurements();
}
