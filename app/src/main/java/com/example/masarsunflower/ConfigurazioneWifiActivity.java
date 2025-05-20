package com.example.masarsunflower;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ConfigurazioneWifiActivity extends AppCompatActivity {

    private static final int REQUEST_WIFI_PERMISSION = 1;
    private static final String TAG = "MasarWifi";

    private WifiManager wifiManager;
    private ListView wifiListView;
    private EditText passwordEditText;
    private Button connettiButton;
    private TextView selectedWifiName;
    private CardView passwordCard;
    private String selectedSsid;
    private WifiScanReceiver receiver;

    // Bluetooth
    private static OutputStream bluetoothOutputStream = null;
    private static boolean bluetoothReady = false;

    public static void setBluetoothOutputStreamStatic(OutputStream outputStream) {
        bluetoothOutputStream = outputStream;
        bluetoothReady = outputStream != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurazione_wifi);

        wifiListView = findViewById(R.id.wifiListView);
        passwordEditText = findViewById(R.id.passwordEditText);
        connettiButton = findViewById(R.id.connettiButton);
        selectedWifiName = findViewById(R.id.selectedWifiName);
        passwordCard = findViewById(R.id.passwordCard);

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "Wi-Fi disabilitato, tentando di attivarlo...");
            Toast.makeText(this, "Wi-Fi disabilitato. Attivando...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
            startActivityForResult(intent, 1);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_WIFI_PERMISSION);
            } else {
                scanWifiNetworks();
            }
        }

        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedSsid = (String) parent.getItemAtPosition(position);

            List<String> selectedList = new ArrayList<>();
            selectedList.add(selectedSsid);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, selectedList);
            wifiListView.setAdapter(adapter);

            selectedWifiName.setText("Rete selezionata: " + selectedSsid);
            passwordCard.setVisibility(View.VISIBLE);
            passwordCard.setAlpha(0f);
            passwordCard.animate().alpha(1f).setDuration(300).start();
        });

        connettiButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Inserisci la password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SSID: " + selectedSsid + "\nPassword: " + password, Toast.LENGTH_LONG).show();

                if (bluetoothReady && bluetoothOutputStream != null) {
                    new Thread(() -> {
                        try {
                            String ssidCommand = "SSID:" + selectedSsid + "\n";
                            Log.d(TAG, "Invio via Bluetooth: " + ssidCommand);
                            bluetoothOutputStream.write(ssidCommand.getBytes());
                            bluetoothOutputStream.flush();
                            Thread.sleep(500);

                            String passCommand = "PASS:" + password + "\n";
                            Log.d(TAG, "Invio via Bluetooth: " + passCommand);
                            bluetoothOutputStream.write(passCommand.getBytes());
                            bluetoothOutputStream.flush();
                            Thread.sleep(500);

                            String connectCommand = "CONNETTI\n";
                            Log.d(TAG, "Invio via Bluetooth: " + connectCommand);
                            bluetoothOutputStream.write(connectCommand.getBytes());
                            bluetoothOutputStream.flush();
                        } catch (IOException | InterruptedException e) {
                            Log.e(TAG, "Errore invio dati Bluetooth", e);
                        }
                    }).start();
                } else {
                    Log.e(TAG, "Bluetooth non connesso");
                    Toast.makeText(this, "Bluetooth non connesso", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void scanWifiNetworks() {
        Log.d(TAG, "Avviando la scansione Wi-Fi...");
        receiver = new WifiScanReceiver(wifiManager, wifiListView);
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        boolean success = wifiManager.startScan();
        if (!success) {
            Log.e(TAG, "Errore nella scansione Wi-Fi");
            Toast.makeText(this, "Errore nella scansione Wi-Fi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            Log.d(TAG, "Deregistrazione del receiver Wi-Fi");
            unregisterReceiver(receiver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WIFI_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permesso Wi-Fi concesso");
                scanWifiNetworks();
            } else {
                Log.d(TAG, "Permesso Wi-Fi negato");
                Toast.makeText(this, "Permesso Wi-Fi necessario", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
