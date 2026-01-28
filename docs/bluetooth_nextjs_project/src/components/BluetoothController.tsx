'use client';

import { useBluetoothStore } from '../store/bluetoothStore';
import { startScan, connect, disconnect } from '../services/bluetoothService';

export default function BluetoothController() {
  const { isScanning, connectedDevice } = useBluetoothStore();

  return (
    <div className="flex flex-col items-center p-4">
      <div className="flex space-x-4">
        <button
          className="px-4 py-2 font-bold text-white bg-blue-500 rounded hover:bg-blue-700"
          onClick={startScan}
          disabled={isScanning}
        >
          {isScanning ? 'Scanning...' : 'Scan'}
        </button>
        <button
          className="px-4 py-2 font-bold text-white bg-green-500 rounded hover:bg-green-700"
          onClick={() => connectedDevice && connect(connectedDevice)}
          disabled={!connectedDevice || connectedDevice.gatt.connected}
        >
          Connect
        </button>
        <button
          className="px-4 py-2 font-bold text-white bg-red-500 rounded hover:bg-red-700"
          onClick={() => connectedDevice && disconnect(connectedDevice)}
          disabled={!connectedDevice || !connectedDevice.gatt.connected}
        >
          Disconnect
        </button>
      </div>
    </div>
  );
}
