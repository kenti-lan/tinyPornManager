package com.scott.pornhub.services;

import com.scott.pornhub.entities.Configuration;
import com.scott.pornhub.entities.Jobs;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ConfigurationService {

    /**
     * Get the system wide configuration information for images. Some elements of the API require
     * some knowledge of the configuration data which can be found here. The purpose of this is to
     * try and keep the actual API responses as light as possible. It is recommended you store this
     * data within your application and check for updates every so often.<br> <br> To build an image
     * URL, you will need 3 pieces of data. The base_url, size and file_path. Simply combine them
     * all and you will have a fully qualified URL. Here is an example URL:<br> <br> <a href=
     * "http://cf2.imgobject.com/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg"
     * >http://cf2.imgobject.com/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg</a>
     */
    @GET("configuration")
    Call<Configuration> configuration();

    /**
     * Get a list of the jobs and departments we use on TMDb.
     *
     * https://developers.themoviedb.org/3/configuration/get-jobs-1
     */
    @GET("configuration/jobs")
    Call<List<Jobs>> jobs();

}
