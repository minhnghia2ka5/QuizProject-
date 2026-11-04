
package com.example.quizproject.Network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface ApiService {

    @GET("categories")
    Call<JsonArray> getCategories();

    @GET("attempts/history")
    Call<JsonArray> getHistory(
            @Header("Authorization") String token
    );

    @GET("attempts/{id}")
    Call<JsonObject> getAttemptDetail(
            @Header("Authorization") String token,
            @Path("id") int attemptId
    );
}
