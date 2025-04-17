package com.example.app_technical_admin.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.androidnetworking.error.ANError;
import com.example.app_technical_admin.R;

import org.json.JSONException;
import org.json.JSONObject;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

public class JoinActivity extends AppCompatActivity {
    private String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcGlrZXkiOiJjZjJkYTRkMC1iODc5LTQyYTUtYWUyYS1mMTQ4YjI3OWRkOWEiLCJwZXJtaXNzaW9ucyI6WyJhbGxvd19qb2luIl0sImlhdCI6MTc0MjkwNTk1OSwiZXhwIjoxNzU4NDU3OTU5fQ.DTv0HfYLE4EiCB-3c97TLQ2ocTdTAyrCZNvbQkXP9pQ";
    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID);
        checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID);

        final Button btnCreate = findViewById(R.id.btnCreateMeeting);
        final Button btnJoinHost = findViewById(R.id.btnJoinHostMeeting);
        final EditText etMeetingId = findViewById(R.id.etMeetingId);

        // create meeting and join as Host
        btnCreate.setOnClickListener(v -> createMeeting(token));

        // Join as Host
        btnJoinHost.setOnClickListener(v -> {
            Intent intent = new Intent(JoinActivity.this, OrderStatusActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("meetingId", etMeetingId.getText().toString().trim());
            intent.putExtra("mode", "CONFERENCE");
            intent.putExtra("participantMode", "SEND_AND_RECV"); // Thêm participantMode
            startActivity(intent);
        });
    }

    private void createMeeting(String token) {
        // we will make an API call to VideoSDK Server to get a roomId
        AndroidNetworking.post("https://api.videosdk.live/v2/rooms")
                .addHeaders("Authorization", token) //we will pass the token in the Headers
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // response will contain `roomId`
                            final String meetingId = response.getString("roomId");

                            // starting the MeetingActivity with received roomId and our sampleToken
                            Intent intent = new Intent(JoinActivity.this, OrderStatusActivity.class);
                            intent.putExtra("token", token);
                            intent.putExtra("meetingId", meetingId);
                            intent.putExtra("mode", "CONFERENCE");
                            intent.putExtra("participantMode", "SEND_AND_RECV");// Sửa thành CONFERENCE
                            startActivity(intent);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        anError.printStackTrace();
                        Toast.makeText(JoinActivity.this, anError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
        }
    }
}