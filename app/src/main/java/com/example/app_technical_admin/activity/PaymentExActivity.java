package com.example.app_technical_admin.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.app_technical_admin.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PaymentExActivity extends Activity {

    private static final String VNPAY_DEMO_URL = "https://sandbox.vnpayment.vn/tryitnow/Home/CreateOrder"; // Thay bằng HTTPS
    private static final String RETURN_URL = "myapp://payment/callback";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_ex);

        Intent intent = getIntent();
        double amount = intent.getDoubleExtra("amount", 0.0);
        String orderInfo = intent.getStringExtra("orderInfo");

        if (amount <= 0 || orderInfo == null || orderInfo.isEmpty()) {
            Toast.makeText(this, "Invalid payment information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        orderInfo = orderInfo.replaceAll("[^a-zA-Z0-9 ]", "").replaceAll("\\s+", " ");
        Log.d("PaymentExActivity", "Cleaned Order Info: " + orderInfo);

        new SubmitOrderTask(amount, orderInfo).execute();
    }

    private class SubmitOrderTask extends AsyncTask<Void, Void, String> {
        private final double amount;
        private final String orderInfo;
        private String errorMessage;

        SubmitOrderTask(double amount, String orderInfo) {
            this.amount = amount;
            this.orderInfo = orderInfo;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                long vnpAmount = (long) (amount * 100);
                if (vnpAmount <= 0) {
                    throw new IllegalArgumentException("Amount must be greater than 0");
                }

                String orderId = String.valueOf(System.currentTimeMillis());

                StringBuilder postData = new StringBuilder();
                postData.append("amount=").append(URLEncoder.encode(String.valueOf(vnpAmount), "UTF-8"));
                postData.append("&orderInfo=").append(URLEncoder.encode(orderInfo, "UTF-8"));
                postData.append("&orderId=").append(URLEncoder.encode(orderId, "UTF-8"));
                postData.append("&returnUrl=").append(URLEncoder.encode(RETURN_URL, "UTF-8"));
                postData.append("&bankCode=").append(URLEncoder.encode("NCB", "UTF-8"));
                postData.append("&payType=").append(URLEncoder.encode("1", "UTF-8"));

                byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

                URL url = new URL(VNPAY_DEMO_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(postDataBytes);
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                Log.d("PaymentExActivity", "POST response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.d("PaymentExActivity", "POST response: " + response.toString());

                    if (response.toString().contains("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")) {
                        String redirectUrl = response.toString().split("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")[1].split("\"")[0];
                        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" + redirectUrl;
                    } else {
                        errorMessage = "Không tìm thấy URL thanh toán trong response.";
                        return null;
                    }
                } else {
                    errorMessage = "Lỗi khi gửi yêu cầu: HTTP " + responseCode;
                    return null;
                }
            } catch (Exception e) {
                Log.e("PaymentExActivity", "Error submitting order: " + e.getMessage(), e);
                errorMessage = "Lỗi khi gửi yêu cầu: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String redirectUrl) {
            if (redirectUrl != null) {
                startPayment(redirectUrl);
            } else {
                new AlertDialog.Builder(PaymentExActivity.this)
                        .setTitle("Lỗi")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void startPayment(String paymentUrl) {
        try {
            Log.d("PaymentExActivity", "Starting payment with URL: " + paymentUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(paymentUrl));

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, 100);
            } else {
                Log.e("PaymentExActivity", "No application can handle this URL: " + paymentUrl);
                new AlertDialog.Builder(this)
                        .setTitle("Lỗi")
                        .setMessage("Không có ứng dụng nào để mở trang thanh toán. Vui lòng kiểm tra trình duyệt hoặc mở URL sau trên trình duyệt:\n\n" + paymentUrl)
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        } catch (Exception e) {
            Log.e("PaymentExActivity", "Error starting payment: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khi mở trang thanh toán: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Intent resultIntent = new Intent();
        if (requestCode == 100) {
            if (resultCode == RESULT_OK && data != null) {
                Uri returnUri = data.getData();
                if (returnUri != null) {
                    String responseCode = returnUri.getQueryParameter("vnp_ResponseCode");
                    String transactionNo = returnUri.getQueryParameter("vnp_TransactionNo");
                    resultIntent.putExtra("responseCode", responseCode);
                    resultIntent.putExtra("transactionNo", transactionNo);
                    Log.d("PaymentExActivity", "Response Code: " + responseCode + ", Transaction No: " + transactionNo);

                    if ("00".equals(responseCode)) {
                        Toast.makeText(this, "Thanh toán thành công! Mã giao dịch: " + transactionNo, Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = getVnpayErrorMessage(responseCode);
                        Toast.makeText(this, "Thanh toán thất bại! " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("PaymentExActivity", "Return URI is null");
                    Toast.makeText(this, "Không nhận được kết quả thanh toán", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e("PaymentExActivity", "Result not OK or data is null");
                Toast.makeText(this, "Thanh toán bị hủy hoặc gặp lỗi", Toast.LENGTH_LONG).show();
            }
            setResult(RESULT_OK, resultIntent);
        }
        finish();
    }

    private String getVnpayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "00":
                return "Giao dịch thành công";
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11":
                return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13":
                return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24":
                return "Giao dịch không thành công do: Khách hàng hủy giao dịch.";
            case "51":
                return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            case "99":
                return "Các lỗi khác (lỗi không xác định).";
            default:
                return "Lỗi không xác định: " + responseCode;
        }
    }
}