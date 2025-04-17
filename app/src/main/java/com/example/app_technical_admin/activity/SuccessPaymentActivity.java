package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.airbnb.lottie.LottieAnimationView;
import com.example.app_technical_admin.R;

public class SuccessPaymentActivity extends AppCompatActivity {

    private TextView tvCountdown;
    private AppCompatButton btnReturnHome;
    private CountDownTimer countDownTimer;
    private LottieAnimationView lottieCheckmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        // Initialize views
        lottieCheckmark = findViewById(R.id.lottieCheckmark); // Reference the LottieAnimationView
        tvCountdown = findViewById(R.id.tvCountdown);
        btnReturnHome = findViewById(R.id.btnReturnHome);

        lottieCheckmark.setSpeed(0.5f); // Reduce the speed to 0.5
        lottieCheckmark.playAnimation();

        // Start the countdown timer (5 seconds)
        startCountdownTimer();

        // Set up the button to return to MainActivity immediately
        btnReturnHome.setOnClickListener(v -> {
            countDownTimer.cancel(); // Cancel the timer if the button is clicked
            returnToMainActivity();
        });
    }

    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(5000, 1000) { // 5 seconds, tick every 1 second
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                tvCountdown.setText("Returning to homepage in " + secondsRemaining + " seconds...");
            }

            @Override
            public void onFinish() {
                returnToMainActivity();
            }
        }.start();
    }

    private void returnToMainActivity() {
        Intent intent = new Intent(SuccessPaymentActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel the timer to avoid memory leaks
        }
    }
}