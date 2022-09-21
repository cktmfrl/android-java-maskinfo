package com.example.maskinfo.repository;

import com.example.maskinfo.model.StoreInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MaskService {
//    String BASE_URL = "https://8oi9s0nnth.apigw.ntruss.com/corona19-masks/v1/";
//
//    @GET("storesByGeo/json/?m=5000")
//    Call<StoreInfo> fetchStoreInfo(@Query("lat") double lat,
//                                   @Query("lng") double lng);

    // 공적마스크 API 중단으로 백업 API로 대체하였음
    String BASE_URL = "https://gist.githubusercontent.com/junsuk5/bb7485d5f70974deee920b8f0cd1e2f0/raw/063f64d9b343120c2cb01a6555cf9b38761b1d94/";

    @GET("sample.json")
    Call<StoreInfo> fetchStoreInfo(@Query("lat") double lat,
                                   @Query("lng") double lng);
}
