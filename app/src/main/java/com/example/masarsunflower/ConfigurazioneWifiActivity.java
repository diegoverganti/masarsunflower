package com.example.masarsunflower;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ConfigurazioneWifiActivity extends AppCompatActivity {

    private static final int REQUEST_WIFI_PERMISSION = 1;
    private WifiManager wifiManager;
    private ListView wifiListView;
    private EditText passwordEditText;
    private Button connettiButton;
    private String selectedSsid;

    private static final String TAG = "MasarWifi";  // Aggiungi un tag per i log

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurazione_wifi);

        wifiListView = findViewById(R.id.wifiListView);
        passwordEditText = findViewById(R.id.passwordEditText);
        connettiButton = findViewById(R.id.connettiButton);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // Verifica se il Wi-Fi è attivato
        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "Wi-Fi disabilitato, tentando di attivarlo...");
            Toast.makeText(this, "Wi-Fi disabilitato. Attivando...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
            startActivityForResult(intent, 1);
        } else {
            Log.d(TAG, "Wi-Fi attivato. Scansionando reti...");
            // Verifica i permessi per la scansione delle reti Wi-Fi
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_WIFI_PERMISSION);
            } else {
                scanWifiNetworks();
            }
        }

        // Listener per la selezione SSID
        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedSsid = (String) parent.getItemAtPosition(position);
            wifiListView.setVisibility(View.GONE);
            passwordEditText.setVisibility(View.VISIBLE);
            connettiButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "SSID selezionato: " + selectedSsid, Toast.LENGTH_SHORT).show();
        });

        // Logica pulsante di connessione
        connettiButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Inserisci la password", Toast.LENGTH_SHORT).show();
            } else {
                // Logica per salvare SSID e password
                Toast.makeText(this, "SSID: " + selectedSsid + "\nPassword: " + password, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void scanWifiNetworks() {
        // Registrazione del receiver per la scansione delle reti Wi-Fi
        Log.d(TAG, "Avviando la scansione Wi-Fi...");
        WifiScanReceiver receiver = new WifiScanReceiver(wifiManager, wifiListView);
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // Avvia la scansione Wi-Fi
        boolean success = wifiManager.startScan();
        if (!success) {
            Log.e(TAG, "Errore nella scansione Wi-Fi");
            Toast.makeText(this, "Errore nella scansione Wi-Fi", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Scansione Wi-Fi avviata con successo");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Deregistra il receiver
        Log.d(TAG, "Deregistrazione del receiver Wi-Fi");
        unregisterReceiver(new WifiScanReceiver(wifiManager, wifiListView));
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
