package com.example.masarsunflower;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;

public class BluetoothConnectedActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothConnectedActivity";

    private VideoView videoView;
    private ImageView imageView;
    private TextView attesaText;
    private LottieAnimationView loadingAnimation;
    private CardView cardView;

    private BleService bleService;
    private boolean isBound = false;
    private String deviceMacAddress;

    private final BroadcastReceiver bleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast ricevuto: " + action);
            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
                runOnUiThread(() -> {
                    Toast.makeText(thisContext(), "BLE Connesso", Toast.LENGTH_SHORT).show();
                    attesaText.setText("Dispositivo Connesso");
                });
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                runOnUiThread(() -> {
                    Toast.makeText(thisContext(), "BLE Disconnesso", Toast.LENGTH_SHORT).show();
                    attesaText.setText("Dispositivo Disconnesso");
                });
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BleService.EXTRA_DATA);
                if (data != null) {
                    runOnUiThread(() -> {
                        attesaText.setText("Ricevuto: " + data);
                        showMessage(data);
                    });
                }
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "Service BLE connesso");
            bleService = ((BleService.LocalBinder) binder).getService();
            isBound = true;
            // NON ricongiungersi qui: la connessione viene gestita dal Service stesso
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service BLE disconnesso");
            isBound = false;
            bleService = null;
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_bluetooth_connected);

        videoView      = findViewById(R.id.videoView);
        imageView      = findViewById(R.id.imageView);
        attesaText     = findViewById(R.id.attesaText);
        loadingAnimation = findViewById(R.id.loading_animation);
        cardView       = findViewById(R.id.cardView);

        deviceMacAddress = getIntent().getStringExtra("deviceAddress");
        Log.d(TAG, "MAC address ricevuto: " + deviceMacAddress);

        // Video in loop
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.spunta_connessione);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> { mp.setLooping(true); mp.start(); });

        // Fade-in
        videoView.postDelayed(() -> {
            fadeIn(imageView);
            imageView.postDelayed(() -> fadeIn(cardView), 500);
        }, 3000);

        // *** REGISTRAZIONE RECEIVER ***
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.ACTION_GATT_CONNECTED);
        filter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BleService.ACTION_DATA_AVAILABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ usa la firma a 5 parametri
            registerReceiver(
                    bleUpdateReceiver,
                    filter,
                    /* broadcastPermission */ null,
                    /* scheduler */ new Handler(getMainLooper()),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            // vecchia firma
            registerReceiver(bleUpdateReceiver, filter);
        }
        Log.d(TAG, "BroadcastReceiver registrato");

        // Bind al servizio (la connessione è già partita in CollegamentoActivity)
        Intent svc = new Intent(this, BleService.class);
        boolean ok = bindService(svc, serviceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "bindService => " + ok);
    }

    private Context thisContext() {
        return BluetoothConnectedActivity.this;
    }

    private void fadeIn(View v) {
        v.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(800);
        v.startAnimation(anim);
    }

    private void showMessage(String msg) {
        String dialog = msg.contains("WIFI SI") ? "CONNESSIONE CON IL WIFI SELEZIONATA"
                : msg.contains("WIFI NO") ? "CONNESSIONE SOLO CON BLUETOOTH SELEZIONATA"
                : "Connessione sconosciuta";

        new AlertDialog.Builder(thisContext())
                .setMessage(dialog)
                .setCancelable(false)
                .show();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(thisContext(), ConfigurazioneWifiActivity.class));
            finish();
        }, 2000);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendDataOverBluetooth(String data) {
        if (isBound && bleService != null) {
            boolean success = bleService.write(data);
            Log.d(TAG, success ? "Inviato: "+data : "Invio fallito");
        } else {
            Log.e(TAG, "Service non connesso");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bleUpdateReceiver);
        } catch (Exception ignored) {}
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
