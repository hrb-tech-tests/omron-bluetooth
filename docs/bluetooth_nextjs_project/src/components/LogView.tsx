'use client';

import { useBluetoothStore } from '../store/bluetoothStore';

export default function LogView() {
  const { logs } = useBluetoothStore();

  return (
    <div className="w-full p-4">
      <h2 className="text-lg font-bold">Logs</h2>
      <div className="h-48 p-2 mt-2 overflow-y-scroll border rounded">
        {logs.map((log, index) => (
          <p key={index} className="text-sm">{log}</p>
        ))}
      </div>
    </div>
  );
}
