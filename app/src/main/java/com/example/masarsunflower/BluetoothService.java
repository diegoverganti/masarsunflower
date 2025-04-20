// BluetoothService.java
package com.example.masarsunflower;

import android.bluetooth.BluetoothSocket;

public class BluetoothService {

    private static BluetoothSocket bluetoothSocket;

    public static void setSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
    }

    public static BluetoothSocket getSocket() {
        return bluetoothSocket;
    }

    public static void closeSocket() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
