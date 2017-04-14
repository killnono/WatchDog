package com.example.killnono.watchdog.remote.apiservice;



import com.example.killnono.watchdog.remote.XApiServiceHelper;

import org.json.JSONObject;

import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by Android Studio
 * User: killnono(陈凯)
 * Date: 16/10/28
 * Time: 下午4:28
 * Version: 1.0
 */
public interface RoomApiService {

    String CVS = "/config/cvs";

    /* test */
    String BASE_URL = "http://10.8.8.8:9430";
//    String BASE_URL = "https://android-api-v4-0.yangcong345.com";

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

    @GET(CVS)
    Observable<JSONObject> cvs();


}
