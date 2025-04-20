package com.example.masarsunflower;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class BluetoothConnectedActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageView imageView;
    private TextView attesaText;
    private LottieAnimationView loadingAnimation;
    private CardView cardView;

    private BluetoothSocket bluetoothSocket;
    private Thread bluetoothReadThread;
    private boolean isReading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connected);

        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        attesaText = findViewById(R.id.attesaText);
        loadingAnimation = findViewById(R.id.loading_animation);
        cardView = findViewById(R.id.cardView);

        // Imposta e avvia il video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.spunta_connessione);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.start();
        });

        // Dopo 3 secondi, mostra immagine e poi la CardView
        videoView.postDelayed(() -> {
            fadeInView(imageView);
            imageView.postDelayed(() -> fadeInView(cardView), 500);
        }, 3000);

        // Recupera il socket e avvia l'ascolto
        bluetoothSocket = BluetoothService.getSocket();

        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            startBluetoothListening();
        } else {
            Log.e("BluetoothMessage", "BluetoothSocket è null o non connesso!");
        }
    }

    private void fadeInView(View view) {
        view.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        view.startAnimation(fadeIn);
    }

    private void startBluetoothListening() {
        bluetoothReadThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(bluetoothSocket.getInputStream())
                );
                String line;
                while (isReading && (line = reader.readLine()) != null) {
                    String finalLine = line;  // Rende la variabile effectively final
                    Log.d("BluetoothMessage", "Ricevuto: " + finalLine);

                    // Mostra l'alert dialog con il messaggio ricevuto
                    runOnUiThread(() -> {
                        showMessage(finalLine);
                        attesaText.setText("Ricevuto: " + finalLine);
                    });
                }

            } catch (IOException e) {
                Log.e("BluetoothMessage", "Errore lettura dati", e);
            }
        });
        bluetoothReadThread.start();
    }

    private void showMessage(String message) {
        String dialogMessage = ""; // Messaggio che verrà mostrato nell'AlertDialog

        // Controlla se il messaggio ricevuto è relativo alla connessione Wi-Fi o Bluetooth
        if (message.contains("WIFI SI")) {
            dialogMessage = "CONNESSIONE CON IL WIFI SELEZIONATA";
        } else if (message.contains("WIFI NO")) {
            dialogMessage = "CONNESSIONE SOLO CON BLUETOOTH SELEZIONATA";
        } else {
            dialogMessage = "Connessione sconosciuta";
        }

        // Crea e mostra l'AlertDialog con il messaggio personalizzato
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogMessage)
                .setCancelable(false);

        AlertDialog alert = builder.create();
        alert.show();

        // Dopo 3 secondi, chiudi l'alert e avvia la nuova Activity
        new android.os.Handler().postDelayed(() -> {
            alert.dismiss();
            // Avvia ConfigurazioneWifiActivity
            Intent intent = new Intent(BluetoothConnectedActivity.this, ConfigurazioneWifiActivity.class);
            startActivity(intent);
            finish();  // Facoltativo: chiude questa Activity per evitare di tornare indietro
        }, 2000); // Cambia 2000 in 3000 per 3 secondi
    }


    public void sendDataOverBluetooth(String data) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write((data + "\n").getBytes()); // Aggiunge newline per readLine()
                outputStream.flush();
                Log.d("BluetoothMessage", "Inviato: " + data);
            } catch (IOException e) {
                Log.e("BluetoothMessage", "Errore invio dati", e);
            }
        } else {
            Log.e("BluetoothMessage", "Socket non connesso!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isReading = false;
        if (bluetoothReadThread != null && bluetoothReadThread.isAlive()) {
            bluetoothReadThread.interrupt();
        }
    }
}
