package com.example.uniconnect_iq


import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST



interface RadarApiService {


    @POST("vincular_usuario")
    suspend fun vincularUsuario(
        @Body datos: VinculacionRequest
    ): Response<Map<String,String>>



    companion object {


        private const val BASE_URL_RADAR =
            "http://192.168.10.4:5000/"


        fun crear():RadarApiService {


            return Retrofit.Builder()

                .baseUrl(BASE_URL_RADAR)

                .addConverterFactory(
                    GsonConverterFactory.create()
                )

                .build()

                .create(
                    RadarApiService::class.java
                )

        }

    }

}