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
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  // RFCOMM SPP UUID
    private static final String TAG = "BluetoothDebug"; // Aggiunto per il debug

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collegamento_bluetooth);

        // Trova il VideoView nel layout
        VideoView videoView = findViewById(R.id.videoView);

        // Imposta il percorso del video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_bluetooth);
        videoView.setVideoURI(videoUri);

        // Avvia il video in loop
        videoView.setOnPreparedListener(mp -> mp.setLooping(true));

        // Avvia il video
        videoView.start();

        // Gestione degli insetti di sistema (barra di stato e navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inizializzazione della lista dei dispositivi
        deviceList = new ArrayList<>();
        ListView bluetoothListView = findViewById(R.id.bluetoothListView);

        // Usa l'adapter personalizzato che utilizza il layout list_item.xml
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.deviceName, deviceList);

        bluetoothListView.setAdapter(adapter);


        // Imposta il click listener per selezionare un dispositivo dalla lista
        bluetoothListView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = deviceList.get(position);
            String deviceAddress = deviceInfo.split("\n")[1];
            selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

            // Avvia la connessione Bluetooth
            connectToDevice();
        });

        // Inizializza il BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "BluetoothAdapter inizializzato");

        if (bluetoothAdapter == null) {
            // Il dispositivo non supporta il Bluetooth
            deviceList.add("Bluetooth non supportato");
            adapter.notifyDataSetChanged();
            Log.e(TAG, "Bluetooth non supportato");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                // Se il Bluetooth non è attivato, chiedi di attivarlo
                Log.d(TAG, "Bluetooth non abilitato, richiedo attivazione");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(enableBtIntent, 1);
            } else {
                // Verifica i permessi per la posizione prima di avviare la scansione dei dispositivi
                checkPermissionsAndStartDiscovery();
            }
        }
    }

    private void checkPermissionsAndStartDiscovery() {
        Log.d(TAG, "Verifico i permessi per la scansione Bluetooth e la posizione");

        // Controlla i permessi per la posizione e Bluetooth
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

        // Iniziamo la scansione dei dispositivi Bluetooth
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permesso per la scansione Bluetooth non concesso");
            return;
        }

        bluetoothAdapter.startDiscovery();
        Log.d(TAG, "Scansione dei dispositivi avviata");

        // Creiamo un broadcast receiver per ricevere i dispositivi trovati
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        Log.d(TAG, "Receiver registrato");
    }

    // BroadcastReceiver per ricevere i dispositivi trovati
    // BroadcastReceiver per ricevere i dispositivi trovati
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(CollegamentoBluetoothActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress(); // Indirizzo MAC

                    // Filtra solo i dispositivi con il nome "MASAR SUNFLOWER"
                    if ("MASAR SUNFLOWER".equals(deviceName)) {
                        String deviceEntry = deviceName + "\n" + deviceAddress;

                        // Aggiungi solo se non è già presente
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
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Richiedi i permessi necessari se non sono già concessi
                return;
            }
            Log.d(TAG, "Connessione al dispositivo " + selectedDevice.getName());
            bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            Toast.makeText(this, "Connesso a " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Connessione stabilita con " + selectedDevice.getName());

            // Gestisci la comunicazione con il dispositivo (ad esempio inviare/ricevere dati)
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore di connessione", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Errore di connessione: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permessi concessi, avvio la scoperta dispositivi");
                startDeviceDiscovery();
            } else {
                Log.e(TAG, "Permessi non concessi");
                // Gestire caso in cui i permessi non sono stati concessi
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Registra la disconnessione del broadcast receiver per evitare memory leaks
        if (bluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Scansione Bluetooth annullata");
        }
        unregisterReceiver(receiver);
        Log.d(TAG, "Receiver deregistrato");

        // Disconnetti il socket Bluetooth se connesso
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                Log.d(TAG, "Socket Bluetooth chiuso");
            } catch (IOException e) {
                Log.e(TAG, "Errore durante la chiusura del socket: " + e.getMessage());
            }
        }
    }
}
