package com.example.masarsunflower;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class CollegamentoBluetoothActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList;
    private ArrayAdapter<String> adapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedDevice;
    private Handler handler;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "BluetoothDebug";
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collegamento_bluetooth);

        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_bluetooth);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> mp.setLooping(true));
        videoView.start();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        deviceList = new ArrayList<>();
        ListView bluetoothListView = findViewById(R.id.bluetoothListView);
        adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.deviceName, deviceList);
        bluetoothListView.setAdapter(adapter);

        bluetoothListView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = deviceList.get(position);
            String deviceAddress = deviceInfo.split("\n")[1];
            selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
            connectToDevice();
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "BluetoothAdapter inizializzato");

        if (bluetoothAdapter == null) {
            deviceList.add("Bluetooth non supportato");
            adapter.notifyDataSetChanged();
            Log.e(TAG, "Bluetooth non supportato");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth non abilitato, richiedo attivazione");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivityForResult(enableBtIntent, 1);
            } else {
                checkPermissionsAndStartDiscovery();
            }
        }
    }

    private void checkPermissionsAndStartDiscovery() {
        Log.d(TAG, "Verifico i permessi per la scansione Bluetooth e la posizione");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Permessi Bluetooth o Posizione non concessi");
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    1);
        } else {
            startDeviceDiscovery();
        }
    }

    private void startDeviceDiscovery() {
        Log.d(TAG, "Inizio la scoperta dei dispositivi Bluetooth");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permesso per la scansione Bluetooth non concesso");
            return;
        }

        bluetoothAdapter.startDiscovery();
        Log.d(TAG, "Scansione dei dispositivi avviata");

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        Log.d(TAG, "Receiver registrato");
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(CollegamentoBluetoothActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    if ("MASAR SUNFLOWER".equals(deviceName)) {
                        String deviceEntry = deviceName + "\n" + deviceAddress;
                        if (!deviceList.contains(deviceEntry)) {
                            Log.d(TAG, "Dispositivo trovato: " + deviceName + " - " + deviceAddress);
                            deviceList.add(deviceEntry);
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.d(TAG, "Dispositivo già presente, ignorato");
                        }
                    }
                } else {
                    Log.e(TAG, "Permesso Bluetooth non concesso durante la scoperta");
                }
            }
        }
    };

    private void connectToDevice() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Mostra il ProgressDialog con animazione Lottie
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.progress_dialog, null));  // Usa il layout personalizzato con Lottie
        builder.setCancelable(false);  // Non consentire di chiudere il dialogo
        progressDialog = builder.create();

        // Imposta uno sfondo trasparente per evitare l'area bianca
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        progressDialog.show();

        // Esegui la connessione in un thread separato
        new Thread(() -> {
            try {
                Log.d(TAG, "Connessione al dispositivo " + selectedDevice.getName());
                bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                BluetoothService.setSocket(bluetoothSocket);

                // Esegui il codice UI nel thread principale per aggiornare l'UI
                runOnUiThread(() -> {
                    progressDialog.dismiss();  // Rimuovi il dialogo di progresso
                    Toast.makeText(this, "Connesso a " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Connessione stabilita con " + selectedDevice.getName());

                    // Avvia l'attività di connessione
                    Intent intent = new Intent(this, BluetoothConnectedActivity.class);
                    intent.putExtra("deviceName", selectedDevice.getName());
                    intent.putExtra("deviceAddress", selectedDevice.getAddress());
                    startActivity(intent);
                    finish();  // Chiudi questa attività dopo la connessione
                });

            } catch (IOException e) {
                // Se c'è un errore nella connessione
                runOnUiThread(() -> {
                    progressDialog.dismiss();  // Rimuovi il dialogo di progresso
                    Toast.makeText(this, "Errore di connessione", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Errore di connessione: " + e.getMessage());
                });
            }
        }).start();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permessi concessi, avvio la scoperta dispositivi");
                startDeviceDiscovery();
            } else {
                Log.e(TAG, "Permessi non concessi");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Scansione Bluetooth annullata");
        }
        unregisterReceiver(receiver);
        Log.d(TAG, "Receiver deregistrato");

    }
}
