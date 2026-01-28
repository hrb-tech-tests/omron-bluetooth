'use client';

import { useBluetoothStore } from '../store/bluetoothStore';

export default function JsonDataView() {
  const { jsonData } = useBluetoothStore();

  return (
    <div className="w-full p-4">
      <h2 className="text-lg font-bold">JSON Data</h2>
      <textarea
        className="w-full h-48 p-2 mt-2 border rounded"
        value={jsonData}
        readOnly
      />
    </div>
  );
}
