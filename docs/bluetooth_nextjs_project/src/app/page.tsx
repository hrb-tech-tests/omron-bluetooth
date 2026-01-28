import BluetoothController from '../components/BluetoothController';
import LogView from '../components/LogView';
import JsonDataView from '../components/JsonDataView';

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <h1 className="text-4xl font-bold mb-8">Omron Bluetooth PWA</h1>
      <BluetoothController />
      <div className="flex w-full max-w-4xl flex-col items-center justify-center">
        <LogView />
        <JsonDataView />
      </div>
    </main>
  );
}
