package com.example.killnono.watchdog.remote.apiservice;


import com.example.killnono.watchdog.remote.XApiServiceHelper;

import org.json.JSONObject;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Android Studio
 * User: killnono(陈凯)
 * Date: 16/10/28
 * Time: 下午4:28
 * Version: 1.0
 */
public interface RoomApiService {

    String ROOM = "room";

    String CURRENT_STATE = "currentState";

    /* test */
    String BASE_URL = "http://116.62.144.229:12345";

    class Factory {
        private static RoomApiService sCourseApiService;

        public static synchronized RoomApiService getInstance() {
            if (sCourseApiService == null) {
                sCourseApiService = create();
            }
            return sCourseApiService;
        }

        private static final RoomApiService create() {
            return XApiServiceHelper.create(RoomApiService.class, BASE_URL);
        }
    }


    @POST(CURRENT_STATE)
    Observable<JSONObject> postCurrentState(@Body JSONObject jsonObject);

    @GET(ROOM)
    Observable<JSONObject> getSelf(@Query("roomId") String roomId );

}
