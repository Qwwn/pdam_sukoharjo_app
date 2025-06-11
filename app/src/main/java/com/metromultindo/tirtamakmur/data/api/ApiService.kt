package com.metromultindo.tirtamakmur.data.api

import com.metromultindo.tirtamakmur.Keys
import com.metromultindo.tirtamakmur.data.model.ComplaintResponse
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import com.metromultindo.tirtamakmur.data.model.FCMResponse
import com.metromultindo.tirtamakmur.data.model.NewsResponse
import com.metromultindo.tirtamakmur.data.model.SelfMeterResponse
import com.metromultindo.tirtamakmur.data.model.SingleNewsResponse
import com.metromultindo.tirtamakmur.data.model.UpdatePhoneResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    @FormUrlEncoded
    @POST("/servicesWithCmp/requestTagihanUnlimited")
    suspend fun getBillInfo(
        @Field("nosambung") customerNumber: String,
        @Header("Authorization") auth: String = Keys.API_KEY
    ): CustomerResponse

    @Multipart
    @POST("/servicesWithCmp/complaint")
    suspend fun submitComplaint(
        @Header("Authorization") auth: String = Keys.API_KEY,
        @Part("name") name: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part("phone") phone: RequestBody?,
        @Part("message") message: RequestBody?,
        @Part("is_customer") isCustomer: RequestBody?,
        @Part("cust_code") customerNo: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part image: MultipartBody.Part?
    ): ComplaintResponse

    // News endpoints
    @GET("/servicesWithCmp/news")
    suspend fun getAllNews(
        @Header("Authorization") auth: String = Keys.API_KEY,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("status") status: Int? = null,
        @Query("category") category: String? = null
    ): NewsResponse

    @GET("/servicesWithCmp/news/{id}")
    suspend fun getNewsById(
        @Path("id") newsId: Int,
        @Header("Authorization") auth: String = Keys.API_KEY
    ): SingleNewsResponse

    // FCM Token endpoints
    @Multipart
    @POST("/servicesWithCmp/fcm/register")
    suspend fun registerFCMToken(
        @Header("Authorization") auth: String = Keys.API_KEY,
        @Part("token") token: RequestBody,
        @Part("device_id") deviceId: RequestBody,
        @Part("device_type") deviceType: RequestBody
    ): FCMResponse

    @Multipart
    @POST("/servicesWithCmp/fcm/delete")
    suspend fun deleteFCMToken(
        @Header("Authorization") auth: String = Keys.API_KEY,
        @Part("token") token: RequestBody,
        @Part("device_id") deviceId: RequestBody
    ): FCMResponse

    @GET("/servicesWithCmp/fcm/test")
    suspend fun testFCMNotification(
        @Header("Authorization") auth: String = Keys.API_KEY,
        @Query("token") token: String,
        @Query("title") title: String = "Test Notification",
        @Query("body") body: String = "This is a test notification"
    ): FCMResponse

    @Multipart
    @POST("/servicesWithCmp/self-meter")
    suspend fun submitSelfMeterReading(
        @Header("Authorization") auth: String = Keys.API_KEY,
        @Part("cust_code") custCode: RequestBody,
        @Part("self_standmtr") standMeter: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part meter_image: MultipartBody.Part?
    ): SelfMeterResponse

    @FormUrlEncoded
    @POST("/servicesWithCmp/updateCustomerPhone")
    suspend fun updateCustomerPhone(
        @Field("nosambung") customerNumber: String,
        @Field("phone") phone: String,
        @Header("Authorization") auth: String = Keys.API_KEY
    ): UpdatePhoneResponse
}