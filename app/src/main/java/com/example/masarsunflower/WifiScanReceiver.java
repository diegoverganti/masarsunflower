package com.example.masarsunflower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {

    private final WifiManager wifiManager;
    private final ListView listView;
    private static final String TAG = "MasarWifi";  // Modificato il tag per i log

    public WifiScanReceiver(WifiManager wifiManager, ListView listView) {
        this.wifiManager = wifiManager;
        this.listView = listView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verifica se l'evento è quello della scansione Wi-Fi
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "Risultati della scansione Wi-Fi ricevuti");

            // Ottiene i risultati della scansione Wi-Fi
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permesso non concesso per la scansione Wi-Fi");
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            List<String> ssids = new ArrayList<>();

            for (ScanResult result : results) {
                if (!ssids.contains(result.SSID) && !result.SSID.isEmpty()) {
                    ssids.add(result.SSID);
                }
            }

            // Log del numero di SSID trovati
            Log.d(TAG, "Reti Wi-Fi trovate: " + ssids.size());

            // Se ci sono SSID, aggiorna la ListView
            if (!ssids.isEmpty()) {
                // Usa un adattatore che collega il layout 'wifi_list_item' al ListView
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                        R.layout.wifi_list_item, // Usa 'wifi_list_item.xml' come layout
                        R.id.wifiSsidText, // Collega al TextView dell'SSID
                        ssids); // La lista di SSID
                listView.setAdapter(adapter);
            } else {
                Log.d(TAG, "Nessuna rete Wi-Fi trovata");
            }
        }
    }
}
