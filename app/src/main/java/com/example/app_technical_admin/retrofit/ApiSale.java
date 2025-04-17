package com.example.app_technical_admin.retrofit;

import com.example.app_technical_admin.model.AdvetisingModel;
import com.example.app_technical_admin.model.ChatAIHistoryResponse;
import com.example.app_technical_admin.model.ChatAIResponse;
import com.example.app_technical_admin.model.MessageModel;
import com.example.app_technical_admin.model.NewProductModel;
import com.example.app_technical_admin.model.OrderModel;
import com.example.app_technical_admin.model.OrderResponse;
import com.example.app_technical_admin.model.PromotionModel;
import com.example.app_technical_admin.model.StatisticModel;
import com.example.app_technical_admin.model.TypeOfProductModel;
import com.example.app_technical_admin.model.UserModel;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiSale {
    @GET("gettypeOfProduct.php")
    Flowable<TypeOfProductModel> getTypeOfProduct();

    @GET("getNewProduct.php")
    Observable<NewProductModel> getNewProduct();

    @GET("getNewProduct.php")
    Observable<NewProductModel> getNewProductByCategory(@Query("category") int category);


    @POST("detail.php")
    @FormUrlEncoded
    Observable<NewProductModel> getProduct(
            @Field("page") int page,
            @Field("category") int category
    );

    @POST("register.php")
    @FormUrlEncoded
    Observable<UserModel> register(
            @Field("email") String email,
            @Field("password") String pass,
            @Field("userName") String userName,
            @Field("phoneNumber") String phoneNumber,
            @Field("uid") String uid
    );

    @POST("login.php")
    @FormUrlEncoded
    Observable<UserModel> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("update_password.php")
    Call<ResponseBody> updatePassword(@Field("email") String email, @Field("password") String password);

    @FormUrlEncoded
    @POST("order.php")
    Observable<MessageModel> createOrder(
            @Field("email") String email,
            @Field("phoneNumber") String phoneNumber,
            @Field("total") String total,
            @Field("id_user") int id_user,
            @Field("address") String address,
            @Field("count") int count,
            @Field("orderdetail") String orderdetail
    );

    @FormUrlEncoded
    @POST("viewOrder.php")
    Observable<OrderModel> viewOrder(
            @Field("id_user") int id_user

    );

    @FormUrlEncoded
    @POST("viewOrderUser.php")
    Observable<OrderModel> viewOrderUser(
            @Field("id_user") int id_user,
            @Query("status") int status

    );

    @FormUrlEncoded
    @POST("search.php")
    Observable<NewProductModel> search(
            @Field("search") String search

    );

    @FormUrlEncoded
    @POST("insertProduct.php")
    Observable<MessageModel> insertProduct(
            @Field("productName") String productName,
            @Field("price") String price,
            @Field("image") String image,
            @Field("description") String description,
            @Field("category") int category,
            @Field("countStock") int countStock,
            @Field("linkVideo") String linkVideo
    ); // Thêm dấu ); để hoàn thành định nghĩa phương thức

    @Multipart
    @POST("upload.php")
    Call<MessageModel> uploadfile(@Part MultipartBody.Part file);

    @FormUrlEncoded
    @POST("delete.php")
    Observable<MessageModel> deleteProduct(
            @Field("id") int id

    );

    @FormUrlEncoded
    @POST("updateProduct.php")
    Observable<MessageModel> updateProduct(
            @Field("productName") String productName,
            @Field("price") String price,
            @Field("image") String image,
            @Field("description") String description,
            @Field("category") int category,
            @Field("countStock") int countStock,
            @Field("id") int id,
            @Field("linkVideo") String linkVideo
    );

    @FormUrlEncoded
    @POST("updateToken.php")
    Observable<MessageModel> updateToken(
            @Field("id") int id,
            @Field("token") String token

    );

    @FormUrlEncoded
    @POST("getToken.php")
    Observable<UserModel> getToken(
            @Field("status") int status


    );

    @GET("getTokenAdmin.php")
    Observable<UserModel> getTokenAdmin(
            @Query("id") int idUser
    );

    @FormUrlEncoded
    @POST("updateOrder.php")
    Observable<MessageModel> updateOrder(

            @Field("id") int id,
            @Field("status") int status

    );

    @FormUrlEncoded
    @POST("updateMomo.php")
    Observable<MessageModel> updateMomo(
            @Field("id_order") int id_order,
            @Field("token") String token

    );
//
//    @FormUrlEncoded
//    @POST("updateMomo.php")
//    Observable<Response<String>> updateMomo(
//            @Field("id_order") int id_order,
//            @Field("token") String token
//    );

    @GET("statistic.php")
    Observable<StatisticModel> getStatistic();

    @GET("monthStatistic.php")
    Observable<StatisticModel> getMonthStatistic();

    @GET("advertising.php")
    Observable<AdvetisingModel> getAdvertising();

    @FormUrlEncoded
    @POST("deleteOrder.php")
    Observable<MessageModel> deleteOrder(
            @Field("id_order") int id

    );

    @FormUrlEncoded
    @POST("insertMeeting.php")
    Observable<MessageModel> postMeeting(
            @Field("meetingId") String meetingId,
            @Field("token") String token

    );



    @FormUrlEncoded
    @POST("getUser.php")
    Observable<UserModel> getUserInfo(
            @Field("userId") String userId
    );

    @FormUrlEncoded
    @POST("update_user.php")
    Call<UserModel> updateUserInfo(
            @Field("userId") String userId,
            @Field("userName") String userName,
            @Field("email") String email,
            @Field("phoneNumber") String phoneNumber,
            @Field("gender") String gender,
            @Field("birthday") String birthday,
            @Field("avatar") String avatar

    );

    @POST("postHlsUrl")
    Single<MessageModel> postHlsUrl(@Query("meetingId") String meetingId, @Query("hlsUrl") String hlsUrl);

    @FormUrlEncoded
    @POST("save_chat.php")
    Call<ChatAIResponse> saveChat(
            @Field("user_id") int userId,
            @Field("message_content") String messageContent,
            @Field("message_type") int messageType,
            @Field("product_ids") String productIds // Thêm trường product_ids
    );

    // API lấy lịch sử trò chuyện
    @GET("get_chat_history.php")
    Call<ChatAIHistoryResponse> getChatHistory(
            @Query("user_id") int userId
    );

    @FormUrlEncoded
    @POST("viewOrder.php")
    Observable<OrderResponse> viewOrderAI(
            @Field("id_user") int id_user

    );

    @FormUrlEncoded
    @POST("update_address.php")
    Observable<MessageModel> updateAddress(
            @Field("user_id") int userId,
            @Field("address") String address
    );

    @FormUrlEncoded
    @POST("clear_chatAI.php")
    Call<ChatAIResponse> clearChat(
            @Field("user_id") int userId
    );

    @GET("get_promotions.php")
    Observable<PromotionModel> getPromotions();


    @FormUrlEncoded
    @POST("send_message.php")
    Observable<String> sendMessage(
            @Field("sender_id") int senderId,
            @Field("receiver_id") Integer receiverId,
            @Field("message") String message
    );

    @FormUrlEncoded
    @POST("get_messages.php")
    Observable<String> getMessages(
            @Field("user_id") int userId,
            @Field("other_id") int otherId
    );

    @FormUrlEncoded
    @POST("get_admins.php")
    Observable<String> getAdmins(
            @Field("admin_id") int adminId
    );

}

