package com.union.accounting.api;

import com.union.accounting.model.ActivityDetail;
import com.union.accounting.model.Administrator;
import com.union.accounting.model.ApiResponse;
import com.union.accounting.model.GroupActivity;
import com.union.accounting.model.OrdinaryUser;
import com.union.accounting.model.Pay;
import com.union.accounting.model.PaymentDetail;
import com.union.accounting.model.Report;

import java.math.BigDecimal;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AccountingApi {
    @FormUrlEncoded
    @POST("api/ordinaryUserLogin")
    Call<ApiResponse<OrdinaryUser>> login(@Field("account") String account,
                                           @Field("userPassword") String password);

    @FormUrlEncoded
    @POST("api/administratorLogin")
    Call<ApiResponse<Administrator>> administratorLogin(@Field("userID") String userID,
                                                        @Field("userPassword") String password);

    @FormUrlEncoded
    @POST("api/sendSmsLoginCode")
    Call<ApiResponse<Void>> sendSmsLoginCode(@Field("phoneNumber") String phoneNumber);

    @FormUrlEncoded
    @POST("api/ordinaryUserSmsLogin")
    Call<ApiResponse<OrdinaryUser>> smsLogin(@Field("phoneNumber") String phoneNumber,
                                             @Field("verifyCode") String verifyCode);

    @FormUrlEncoded
    @POST("api/signup")
    Call<ApiResponse<Void>> register(@Field("userName") String userName,
                                     @Field("userPassword") String password,
                                     @Field("confirmPassword") String confirmPassword,
                                     @Field("phoneNumber") String phoneNumber,
                                     @Field("userMailbox") String email,
                                     @Field("verifyCode") String verifyCode);

    @FormUrlEncoded
    @POST("api/forgetPassword")
    Call<ApiResponse<Void>> forgetPassword(@Field("phoneNumber") String phoneNumber,
                                           @Field("verifyCode") String verifyCode,
                                           @Field("newPassword") String newPassword,
                                           @Field("confirmPassword") String confirmPassword);

    @POST("api/logout")
    Call<ApiResponse<Void>> logout();

    @GET("api/user/me")
    Call<ApiResponse<OrdinaryUser>> me();

    @FormUrlEncoded
    @POST("api/user/update")
    Call<ApiResponse<OrdinaryUser>> updateUser(@Field("userName") String userName,
                                               @Field("phoneNumber") String phoneNumber,
                                               @Field("userMailbox") String userMailbox,
                                               @Field("realName") String realName,
                                               @Field("gender") String gender,
                                               @Field("birthday") String birthday);

    @FormUrlEncoded
    @POST("api/user/password")
    Call<ApiResponse<Void>> changePassword(@Field("oldPassword") String oldPassword,
                                           @Field("newPassword") String newPassword,
                                           @Field("confirmPassword") String confirmPassword);

    @POST("api/user/delete")
    Call<ApiResponse<Void>> deleteCurrentUser();

    @GET("api/activity/list")
    Call<ApiResponse<List<GroupActivity>>> activities(@Query("keyword") String keyword,
                                                      @Query("isSettable") Boolean isSettable,
                                                      @Query("myCreatedUnsettled") Boolean myCreatedUnsettled);

    @GET("api/activity/detail")
    Call<ApiResponse<ActivityDetail>> activityDetail(@Query("activityID") Integer activityID);

    @FormUrlEncoded
    @POST("api/activity/create")
    Call<ApiResponse<GroupActivity>> createActivity(@Field("activityName") String activityName,
                                                    @Field("description") String description,
                                                    @Field("actualAmount") BigDecimal actualAmount,
                                                    @Field("maxMember") Integer maxMember,
                                                    @Field("inviteCodes") List<String> inviteCodes);

    @FormUrlEncoded
    @POST("api/activity/add-member")
    Call<ApiResponse<Void>> addMember(@Field("activityID") Integer activityID,
                                      @Field("inviteCode") String inviteCode);

    @FormUrlEncoded
    @POST("api/activity/update")
    Call<ApiResponse<GroupActivity>> updateActivity(@Field("activityID") Integer activityID,
                                                    @Field("activityName") String activityName,
                                                    @Field("description") String description,
                                                    @Field("actualAmount") BigDecimal actualAmount,
                                                    @Field("maxMember") Integer maxMember);

    @FormUrlEncoded
    @POST("api/activity/delete")
    Call<ApiResponse<Void>> deleteActivity(@Field("activityID") Integer activityID);

    @FormUrlEncoded
    @POST("api/activity/remove-member")
    Call<ApiResponse<Void>> removeMember(@Field("activityID") Integer activityID,
                                         @Field("memberID") Integer memberID,
                                         @Field("removeReason") String removeReason);

    @FormUrlEncoded
    @POST("api/activity/handle-quit")
    Call<ApiResponse<Void>> handleQuit(@Field("activityID") Integer activityID,
                                       @Field("memberID") Integer memberID,
                                       @Field("allowQuit") boolean allowQuit);

    @FormUrlEncoded
    @POST("api/activity/init-confirm")
    Call<ApiResponse<Void>> initConfirm(@Field("activityID") Integer activityID);

    @FormUrlEncoded
    @POST("api/activity/confirm")
    Call<ApiResponse<Void>> confirm(@Field("activityID") Integer activityID);

    @FormUrlEncoded
    @POST("api/activity/apply-quit")
    Call<ApiResponse<Void>> applyQuit(@Field("activityID") Integer activityID);

    @FormUrlEncoded
    @POST("api/settlement/init")
    Call<ApiResponse<Void>> initSettlement(@Field("activityID") Integer activityID,
                                           @Field("description") String description);

    @FormUrlEncoded
    @POST("api/settlement/batch")
    Call<ApiResponse<Void>> batchSettlement(@Field("activityIDs") List<Integer> activityIDs,
                                            @Field("description") String description);

    @GET("api/settlement/my-pays")
    Call<ApiResponse<List<Pay>>> myPays();

    @GET("api/settlement/pay-detail")
    Call<ApiResponse<PaymentDetail>> payDetail(@Query("payID") Integer payID);

    @Multipart
    @POST("api/settlement/pay/finish")
    Call<ApiResponse<Void>> finishPayment(@Part("payID") RequestBody payID,
                                          @Part("payRemark") RequestBody payRemark,
                                          @Part("payMethod") RequestBody payMethod,
                                          @Part MultipartBody.Part paymentProof);

    @Multipart
    @POST("api/user/payment-qrcode")
    Call<ApiResponse<OrdinaryUser>> uploadPaymentQrCode(@Part MultipartBody.Part paymentQrCode);

    @GET("api/report/my")
    Call<ApiResponse<List<Report>>> myReports();

    @GET("api/report/warnings")
    Call<ApiResponse<List<Report>>> warnings();

    @FormUrlEncoded
    @POST("api/report/publish")
    Call<ApiResponse<Void>> publishReport(@Field("accusedActivityID") Integer activityID,
                                          @Field("accusedUserKeyword") String accusedUserKeyword,
                                          @Field("reportReason") String reason);

    @FormUrlEncoded
    @POST("api/report/cancel")
    Call<ApiResponse<Void>> cancelReport(@Field("reportID") Integer reportID);

    @GET("api/user/list")
    Call<ApiResponse<List<OrdinaryUser>>> adminUsers();

    @FormUrlEncoded
    @POST("api/user/admin/delete")
    Call<ApiResponse<Void>> adminDeleteUser(@Field("userID") Integer userID);

    @GET("api/report/admin/search")
    Call<ApiResponse<List<Report>>> adminReports(@Query("keyword") String keyword,
                                                 @Query("reportProcessStatus") String reportProcessStatus);

    @FormUrlEncoded
    @POST("api/report/admin/audit")
    Call<ApiResponse<Void>> adminAuditReport(@Field("reportID") Integer reportID,
                                             @Field("reportProcessStatus") String reportProcessStatus,
                                             @Field("punishmentType") String punishmentType);
}
