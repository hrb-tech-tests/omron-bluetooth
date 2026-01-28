import { create } from 'zustand';

interface BluetoothState {
  isScanning: boolean;
  scanResults: any[];
  connectedDevice: any;
  logs: string[];
  jsonData: string;
  startScan: () => void;
  stopScan: () => void;
  connect: (device: any) => void;
  disconnect: () => void;
  addLog: (log: string) => void;
  setJsonData: (data: any) => void;
}

export const useBluetoothStore = create<BluetoothState>((set) => ({
  isScanning: false,
  scanResults: [],
  connectedDevice: null,
  logs: [],
  jsonData: '',
  startScan: () => set({ isScanning: true }),
  stopScan: () => set({ isScanning: false }),
  connect: (device) => set({ connectedDevice: device }),
  disconnect: () => set({ connectedDevice: null }),
  addLog: (log) => set((state) => ({ logs: [...state.logs, log] })),
  setJsonData: (data) => set({ jsonData: JSON.stringify(data, null, 2) }),
}));
