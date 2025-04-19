package com.example.masarsunflower;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import android.view.ViewGroup;
import android.media.MediaPlayer;
import android.media.AudioManager;

public class CaricamentoActivity extends Activity {

    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caricamento);

        // Ottieni l'AudioManager per la gestione dell'audio focus
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Richiedi l'audio focus in modo da non interferire con altre app (come Spotify)
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        // Trova il VideoView nel layout
        VideoView videoView = findViewById(R.id.videoView);

        // Imposta il percorso del video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_caricamento);
        videoView.setVideoURI(videoUri);

        // Avvia la riproduzione del video
        videoView.start();

        // Imposta le dimensioni del video in base alle proporzioni
        videoView.setOnPreparedListener(mp -> {
            // Disabilita solo l'audio del video
            mp.setVolume(0f, 0f);

            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();

            // Imposta una larghezza massima per il video (ad esempio 80% della larghezza dello schermo)
            int maxWidth = getResources().getDisplayMetrics().widthPixels * 8 / 10;

            // Calcola le nuove dimensioni in modo che il video mantenga le proporzioni
            float ratio = (float) videoWidth / (float) videoHeight;
            int newWidth = maxWidth;
            int newHeight = (int) (newWidth / ratio);

            // Applica le nuove dimensioni al VideoView
            ViewGroup.LayoutParams params = videoView.getLayoutParams();
            params.width = newWidth;
            params.height = newHeight;
            videoView.setLayoutParams(params);
        });

        // Quando il video finisce, passa all'activity principale
        videoView.setOnCompletionListener(mp -> {
            Intent intent = new Intent(CaricamentoActivity.this, CollegamentoBluetoothActivity.class);
            startActivity(intent);
            finish();  // Chiudi l'activity di caricamento
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Rilascia l'audio focus quando l'attività è in pausa
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }
}
