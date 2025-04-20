package com.example.masarsunflower;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class ConfigurazioneWifiActivity extends AppCompatActivity {

    private static final int REQUEST_WIFI_PERMISSION = 1;
    private WifiManager wifiManager;
    private ListView wifiListView;
    private EditText passwordEditText;
    private Button connettiButton;
    private String selectedSsid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurazione_wifi);

        wifiListView = findViewById(R.id.wifiListView);
        passwordEditText = findViewById(R.id.passwordEditText);
        connettiButton = findViewById(R.id.connettiButton);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        // Controlla i permessi
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_WIFI_PERMISSION);
        } else {
            scanWifiNetworks();
        }

        // Imposta il listener per la selezione della rete Wi-Fi
        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedSsid = (String) parent.getItemAtPosition(position);

            // Nascondi la lista e mostra la sezione per la password
            wifiListView.setVisibility(View.GONE);
            passwordEditText.setVisibility(View.VISIBLE);
            connettiButton.setVisibility(View.VISIBLE);

            Toast.makeText(this, "SSID selezionato: " + selectedSsid, Toast.LENGTH_SHORT).show();
        });

        // Logica per il pulsante di connessione
        connettiButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Inserisci la password", Toast.LENGTH_SHORT).show();
            } else {
                // Qui puoi salvare SSID e password o avviare il processo di connessione
                Toast.makeText(this, "SSID: " + selectedSsid + "\nPassword: " + password, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Metodo per eseguire la scansione delle reti Wi-Fi
    private void scanWifiNetworks() {
        registerReceiver(new WifiScanReceiver(wifiManager, wifiListView),
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "Errore nella scansione Wi-Fi", Toast.LENGTH_SHORT).show();
        }
    }
}
