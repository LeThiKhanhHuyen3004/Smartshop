package com.example.app_technical_admin.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientNoti {

        private static Retrofit instance;

        public static Retrofit getInstance() {
            if (instance == null) {
                instance = new Retrofit.Builder()
                        .baseUrl("https://fcm.googleapis.com/")
                        .addConverterFactory(GsonConverterFactory.create()) // Sử dụng Gson đã được setLenient
                        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                        .build();
            }
            return instance;
        }

}
