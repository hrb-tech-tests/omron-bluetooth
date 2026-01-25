package com.example.omronapp.mynativebluetooth;

import com.codename1.system.NativeLookup;

public class MyNativeBluetoothFactory {
    private static MyNativeBluetooth instance;

    public static MyNativeBluetooth getInstance() {
        if (instance == null) {
            instance = NativeLookup.create(MyNativeBluetooth.class);
        }
        return instance;
    }
}
