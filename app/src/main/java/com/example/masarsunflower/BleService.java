package com.example.masarsunflower;

import android.Manifest;
import android.app.Service;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.UUID;

public class BleService extends Service {
    private static final String TAG = "BleService";

    // UUID del servizio e delle caratteristiche (sostituisci coi tuoi)
    private static final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHARACTERISTIC_UUID_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHARACTERISTIC_UUID_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    public static final String ACTION_GATT_CONNECTED    = "com.example.masarsunflower.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.example.masarsunflower.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_DATA_AVAILABLE    = "com.example.masarsunflower.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA               = "com.example.masarsunflower.EXTRA_DATA";

    public static final String EXTRA_DEVICE_MAC         = "DEVICE_MAC";

    private final IBinder binder = new LocalBinder();

    private BluetoothGatt            bluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Se il service viene avviato (via startService),
     * leggiamo l’extra DEVICE_MAC e apriamo la connessione GATT.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_DEVICE_MAC)) {
            String mac = intent.getStringExtra(EXTRA_DEVICE_MAC);
            connectDevice(mac, true);
        }
        return START_STICKY;
    }

    /**
     * Connette al device BLE specificato dall’indirizzo MAC.
     * @param macAddress indirizzo MAC del peripheral
     * @param autoConnect se true Android tenterà di ricollegarsi automaticamente
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connectDevice(String macAddress, boolean autoConnect) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || macAddress == null) return;
        BluetoothDevice device = adapter.getRemoteDevice(macAddress);

        // se già connessi ad altro, chiudiamo prima
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        Log.i(TAG, "Connecting to " + macAddress + " autoConnect=" + autoConnect);
        bluetoothGatt = device.connectGatt(this, autoConnect, gattCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean write(String data) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            Log.w(TAG, "write: GATT non pronto o caratteristica RX null");
            return false;
        }
        writeCharacteristic.setValue(data.getBytes());
        return bluetoothGatt.writeCharacteristic(writeCharacteristic);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "GATT connesso");
                broadcast(ACTION_GATT_CONNECTED);
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "GATT disconnesso");
                broadcast(ACTION_GATT_DISCONNECTED);
                close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered errore status=" + status);
                return;
            }
            BluetoothGattService svc = bluetoothGatt.getService(SERVICE_UUID);
            if (svc == null) {
                Log.e(TAG, "Servizio non trovato");
                return;
            }
            writeCharacteristic = svc.getCharacteristic(CHARACTERISTIC_UUID_RX);
            BluetoothGattCharacteristic notifyChar = svc.getCharacteristic(CHARACTERISTIC_UUID_TX);
            if (notifyChar != null) {
                bluetoothGatt.setCharacteristicNotification(notifyChar, true);
                // Qui idealmente si scrive il descriptor CLIENT_CHARACTERISTIC_CONFIG
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHARACTERISTIC_UUID_TX.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    String s = new String(data);
                    broadcast(ACTION_DATA_AVAILABLE, s);
                }
            }
        }
    };

    private void broadcast(String action) {
        Intent i = new Intent(action);
        sendBroadcast(i);
    }

    private void broadcast(String action, String data) {
        Intent i = new Intent(action);
        i.putExtra(EXTRA_DATA, data);
        sendBroadcast(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BleService creato");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        Log.i(TAG, "BleService distrutto");
    }
}
