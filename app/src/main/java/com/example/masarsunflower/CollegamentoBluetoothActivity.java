package com.example.masarsunflower;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.UUID;

public class CollegamentoBluetoothActivity extends AppCompatActivity {

    private static final String TAG = "BLE_DEBUG";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;

    private ArrayList<String> deviceList;
    private ArrayAdapter<String> adapter;
    private ListView bluetoothListView;
    private BluetoothDevice selectedDevice;

    private AlertDialog progressDialog;

    private static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_CHARACTERISTIC_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_CHARACTERISTIC_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collegamento_bluetooth);

        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_bluetooth);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> mp.setLooping(true));
        videoView.start();

        deviceList = new ArrayList<>();
        bluetoothListView = findViewById(R.id.bluetoothListView);
        adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.deviceName, deviceList);
        bluetoothListView.setAdapter(adapter);

        bluetoothListView.setOnItemClickListener((parent, view, position, id) -> {
            stopScan();
            String address = deviceList.get(position).split("\n")[1];
            selectedDevice = bluetoothAdapter.getRemoteDevice(address);
            connectToDevice();
        });

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supportato su questo dispositivo", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            checkPermissionsAndStartScan();
        }
    }

    private void checkPermissionsAndStartScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                startScan();
            }
        }
    }

    private void startScan() {
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permesso BLUETOOTH_SCAN non concesso");
            return;
        }
        bleScanner.startScan(scanCallback);
        Log.d(TAG, "BLE scan started");
    }

    private void stopScan() {
        if (bleScanner != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bleScanner.stopScan(scanCallback);
            Log.d(TAG, "BLE scan stopped");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(CollegamentoBluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permesso BLUETOOTH_CONNECT non concesso");
                return;
            }

            String name = device.getName();
            String address = device.getAddress();
            Log.d(TAG, "Dispositivo trovato: nome=" + name + ", indirizzo=" + address);

            // Filtro robusto ignorando maiuscole/minuscole
            if (name != null && name.equalsIgnoreCase("MASAR SUNFLOWER")) {
                String entry = name + "\n" + address;
                if (!deviceList.contains(entry)) {
                    deviceList.add(entry);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Aggiunto dispositivo: " + entry);
                }
            }
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan BLE fallito con codice: " + errorCode);
        }
    };


    private void connectToDevice() {
        showProgressDialog();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
        bluetoothGatt = selectedDevice.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connesso GATT");
                if (ActivityCompat.checkSelfPermission(CollegamentoBluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "Disconnesso GATT");

                runOnUiThread(() -> {
                    Toast.makeText(CollegamentoBluetoothActivity.this, "Disconnesso", Toast.LENGTH_SHORT).show();
                });

                gatt.close();
                bluetoothGatt = null;

                retryConnection(); // Riconnessione automatica
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID_SERVICE);
            if (service != null) {
                BluetoothGattCharacteristic txChar = service.getCharacteristic(UUID_CHARACTERISTIC_TX);
                BluetoothGattCharacteristic rxChar = service.getCharacteristic(UUID_CHARACTERISTIC_RX);

                if (txChar != null && rxChar != null) {
                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        Toast.makeText(CollegamentoBluetoothActivity.this, "Connesso e pronto", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CollegamentoBluetoothActivity.this, BluetoothConnectedActivity.class);
                        if (ActivityCompat.checkSelfPermission(CollegamentoBluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        intent.putExtra("deviceName", selectedDevice.getName());
                        intent.putExtra("deviceAddress", selectedDevice.getAddress());
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Log.e(TAG, "Caratteristiche non trovate");
                    disconnectGatt();
                }
            } else {
                Log.e(TAG, "Servizio BLE non trovato");
                disconnectGatt();
            }
        }
    };

    private void showProgressDialog() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity non attiva, impossibile mostrare il progress dialog");
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(getLayoutInflater().inflate(R.layout.progress_dialog, null));
            builder.setCancelable(false);
            progressDialog = builder.create();
            if (progressDialog.getWindow() != null) {
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            progressDialog.show();
        });
    }


    private void dismissProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void retryConnection() {
        runOnUiThread(() -> {
            Toast.makeText(CollegamentoBluetoothActivity.this, "Riconnessione in corso...", Toast.LENGTH_SHORT).show();
            connectToDevice();
        });
    }

    private void disconnectGatt() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Disconnessione GATT...", Toast.LENGTH_SHORT).show();
        });

        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothGatt.disconnect(); // non chiudiamo subito, chiusura in onConnectionStateChange()
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissionsAndStartScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            startScan();
        }
    }
}
