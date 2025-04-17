package com.example.app_technical_admin.retrofit;

import io.reactivex.rxjava3.core.Observable;


import com.example.app_technical_admin.model.NotiResponse;
import com.example.app_technical_admin.model.NotiSendData;

import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiPushNotification {
    @Headers({
            "Content-Type: application/json",
            "Authorization: "
    })
    @POST("v1/projects/btecbs00480/messages:send")
    Observable<NotiResponse> sendNotification(@Body NotiSendData data);
}
