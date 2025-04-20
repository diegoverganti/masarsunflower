package com.example.masarsunflower;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;

public class BluetoothConnectedActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageView imageView;
    private TextView attesaText;
    private LottieAnimationView loadingAnimation;
    private CardView cardView;

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

        // Dopo 3 secondi, mostra con fade-in l'immagine
        videoView.postDelayed(() -> {
            fadeInView(imageView);

            // Dopo un altro piccolo ritardo, mostra la CardView con fade-in
            imageView.postDelayed(() -> {
                fadeInView(cardView);
            }, 500);

        }, 3000);
    }

    // Metodo per fade-in generico
    private void fadeInView(View view) {
        view.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        view.startAnimation(fadeIn);
    }
}
