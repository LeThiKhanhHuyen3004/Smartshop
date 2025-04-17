package com.example.app_technical_admin.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.fragment.SpeakerFragment;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.VideoSDK;
import live.videosdk.rtc.android.listeners.MeetingEventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import org.json.JSONObject;

public class OrderStatusActivity extends AppCompatActivity {
    private Meeting meeting;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiSale apiSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meeting);

        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        createMeeting();
    }

    private void createMeeting() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://api.videosdk.live/v2/rooms")
                        .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                        .addHeader("Authorization", "Bearer YOUR_VIDEOSDK_TOKEN") // Thay YOUR_VIDEOSDK_TOKEN bằng token của bạn
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                String meetingId = json.getString("roomId");
                String token = json.getString("token");

                runOnUiThread(() -> {
                    Log.d("MeetingActivity", "Admin - Meeting ID: " + meetingId + ", Token: " + token);
                    initializeMeeting(meetingId, token);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e("MeetingActivity", "Error creating meeting: " + e.getMessage(), e);
                    Toast.makeText(this, "Error creating meeting: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void initializeMeeting(String meetingId, String token) {
        String mode = "CONFERENCE";
        String localParticipantName = "App Technical";
        boolean streamEnable = true; // Mode CONFERENCE

        VideoSDK.initialize(getApplicationContext());
        VideoSDK.config(token);

        meeting = VideoSDK.initMeeting(
                OrderStatusActivity.this, meetingId, localParticipantName,
                streamEnable, streamEnable, null, mode, false, null, null);

        if (meeting != null) {
            meeting.join();
            meeting.addEventListener(new MeetingEventListener() {
                @Override
                public void onMeetingJoined() {
                    if (meeting != null) {
                        meeting.getLocalParticipant().pin("SHARE_AND_CAM");
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainLayout, new SpeakerFragment(), "MainFragment")
                                .commit();
                        postDataToMeeting(meetingId, token);
                    }
                }

                @Override
                public void onError(JSONObject error) {
                    Log.e("MeetingActivity", "Meeting error: " + error.toString());
                    Toast.makeText(OrderStatusActivity.this, "Error joining meeting: " + error.toString(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Log.e("MeetingActivity", "Failed to initialize meeting with ID: " + meetingId);
            Toast.makeText(this, "Failed to initialize meeting. Please check meeting ID and token.", Toast.LENGTH_LONG).show();
        }
    }

    private void postDataToMeeting(String meetingId, String token) {
        compositeDisposable.add(apiSale.postMeeting(meetingId, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            Log.d("log", "post ok");
                            Toast.makeText(this, "Meeting saved successfully", Toast.LENGTH_SHORT).show();
                        },
                        throwable -> {
                            Log.e("log", "Error posting meeting: " + throwable.getMessage(), throwable);
                            Toast.makeText(this, "Error saving meeting: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                ));
    }

    public Meeting getMeeting() {
        return meeting;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        if (meeting != null) {
            meeting.leave();
            meeting = null;
        }
    }
}