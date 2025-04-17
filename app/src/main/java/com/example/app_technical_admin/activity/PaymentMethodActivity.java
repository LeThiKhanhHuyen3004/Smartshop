package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app_technical_admin.R;

public class PaymentMethodActivity extends AppCompatActivity {
    private static final String TAG = "PaymentMethodActivity";
    private RadioGroup radioGroupPayment;
    private Button btnConfirm;
    private RadioButton radioCash, radioMomo, radioZalopay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_method);

        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        btnConfirm = findViewById(R.id.btnConfirm);

        // Get references to the RadioButtons and cast them properly
        radioCash = findViewById(R.id.radioCash);
        radioMomo = findViewById(R.id.radioMomo);
        radioZalopay = findViewById(R.id.radioZalopay);

        // Get the currently selected payment method from the intent (if any)
        String currentMethod = getIntent().getStringExtra("selectedPaymentMethod");
        Log.d(TAG, "Phương thức hiện tại: " + currentMethod);

        // Set the initial selection based on the passed method
        if ("MoMo".equals(currentMethod)) {
            radioMomo.setChecked(true);
        } else if ("ZaloPay".equals(currentMethod)) {
            radioZalopay.setChecked(true);
        } else {
            radioCash.setChecked(true);
        }

        // Log initial state
        logRadioButtonStates("Trạng thái ban đầu");

        // Monitor RadioGroup changes
        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "RadioButton được chọn: " + checkedId);
            logRadioButtonStates("Sau khi thay đổi");
        });

        btnConfirm.setOnClickListener(v -> {
            int selectedId = radioGroupPayment.getCheckedRadioButtonId();
            String selectedMethod = "Cash"; // Default

            if (selectedId == R.id.radioMomo) {
                selectedMethod = "MoMo";
            } else if (selectedId == R.id.radioZalopay) {
                selectedMethod = "ZaloPay";
            }

            Log.d(TAG, "Phương thức được chọn khi xác nhận: " + selectedMethod);
            logRadioButtonStates("Khi xác nhận");

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedPaymentMethod", selectedMethod);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    // Helper method to log the state of all RadioButtons
    private void logRadioButtonStates(String context) {
        Log.d(TAG, context + " - Cash: " + radioCash.isChecked() +
                ", MoMo: " + radioMomo.isChecked() +
                ", ZaloPay: " + radioZalopay.isChecked());
    }
}