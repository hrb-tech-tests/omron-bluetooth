import { useBluetoothStore } from '../store/bluetoothStore';

const OMRON_CUSTOM_SERVICE_UUID = '49123040-aee8-11e1-a74d-0002a5d5c51b';
const OMRON_WRITE_UUID = '49123041-aee8-11e1-a74d-0002a5d5c51b';
const OMRON_NOTIFY_UUID = '49123042-aee8-11e1-a74d-0002a5d5c51b';
const BLOOD_PRESSURE_SERVICE_UUID = '00001810-0000-1000-8000-00805f9b34fb';

export const startScan = async () => {
  const { addLog, startScan, stopScan } = useBluetoothStore.getState();
  addLog('Requesting Bluetooth device...');
  startScan();

  try {
    const device = await navigator.bluetooth.requestDevice({
      filters: [{ services: [BLOOD_PRESSURE_SERVICE_UUID] }],
      optionalServices: [OMRON_CUSTOM_SERVICE_UUID],
    });

    addLog(`Device selected: ${device.name}`);
    useBluetoothStore.getState().connect(device);
  } catch (error) {
    addLog(`⚠️ Error: ${error}`);
  } finally {
    stopScan();
  }
};

export const connect = async (device: any) => {
  const { addLog, setJsonData } = useBluetoothStore.getState();
  addLog('Connecting to GATT Server...');
  try {
    const server = await device.gatt.connect();
    addLog('GATT Server connected.');

    const service = await server.getPrimaryService(OMRON_CUSTOM_SERVICE_UUID);
    addLog('Service retrieved.');

    const notifyCharacteristic = await service.getCharacteristic(OMRON_NOTIFY_UUID);
    const writeCharacteristic = await service.getCharacteristic(OMRON_WRITE_UUID);
    addLog('Characteristics retrieved.');

    await notifyCharacteristic.startNotifications();
    notifyCharacteristic.addEventListener('characteristicvaluechanged', handleNotifications);
    addLog('Subscribed to notifications.');

    const handshake = new Uint8Array([0x00, 0x02, 0x00, 0x10, 0x85, 0x00, 0x00, 0x10, 0x8E]);
    await writeCharacteristic.writeValue(handshake);
    addLog('✅ Handshake sent. Waiting for data...');
  } catch (error) {
    addLog(`⚠️ Error: ${error}`);
  }
};

export const disconnect = (device: any) => {
  if (device && device.gatt.connected) {
    device.gatt.disconnect();
    useBluetoothStore.getState().disconnect();
    useBluetoothStore.getState().addLog('Device disconnected.');
  }
};

const handleNotifications = (event: any) => {
  const { addLog, setJsonData } = useBluetoothStore.getState();
  const value = event.target.value;
  addLog(`Received notification with ${value.byteLength} bytes.`);
  // A real implementation would parse the data and update the jsonData state
  setJsonData({ raw: value });
};
