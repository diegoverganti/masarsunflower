package com.example.masarsunflower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {

    private final WifiManager wifiManager;
    private final ListView listView;

    public WifiScanReceiver(WifiManager wifiManager, ListView listView) {
        this.wifiManager = wifiManager;
        this.listView = listView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verifica se il permesso è stato concesso
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Ottiene i risultati della scansione Wi-Fi
        List<ScanResult> results = wifiManager.getScanResults();
        List<String> ssids = new ArrayList<>();

        for (ScanResult result : results) {
            if (!ssids.contains(result.SSID) && !result.SSID.isEmpty()) {
                ssids.add(result.SSID);
            }
        }

        // Crea un adapter personalizzato con il layout e lo applica alla ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                R.layout.wifi_list_item, R.id.wifiSsidText, ssids);

        listView.setAdapter(adapter);
    }
}
