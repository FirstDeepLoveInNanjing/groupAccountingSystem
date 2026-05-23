package com.union.accounting.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.union.accounting.R;
import com.union.accounting.api.ApiConfig;
import com.union.accounting.api.ApiClient;
import com.union.accounting.model.ActivityDetail;
import com.union.accounting.model.ActivityMember;
import com.union.accounting.model.ApiResponse;
import com.union.accounting.model.GroupActivity;
import com.union.accounting.model.OrdinaryUser;
import com.union.accounting.model.Pay;
import com.union.accounting.model.PaymentDetail;
import com.union.accounting.model.Report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {
    private static final int REQUEST_PAYMENT_QR_CODE = 1001;
    private static final int REQUEST_PAYMENT_PROOF = 1002;
    private static final String STATUS_UNPAID = "未支付";
    private static final String STATUS_UNCONFIRMED = "未确认";
    private static final String STATUS_CONFIRMED = "已确认";
    private static final String STATUS_QUIT_APPLY = "申请退出";
    private static final String STATUS_PENDING_REVIEW = "待审核";
    private static final String ROLE_CREATOR = "创建者";
    private static final String PAY_METHOD_QR_CODE = "收款码转账";
    private static final String PAY_METHOD_THIRD_PART = "第三方支付";

    private LinearLayout contentLayout;
    private TextView titleText;
    private Integer pendingPayId;
    private String pendingPayRemark;
    private String pendingPayMethod;
    private String activityKeyword;
    private Boolean activityIsSettable;
    private Boolean activityMyCreatedUnsettled;
    private String adminUserKeyword;
    private List<GroupActivity> currentActivities = new ArrayList<>();
    private boolean adminMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ApiClient.init(this);
        adminMode = getIntent().getBooleanExtra("adminMode", false);
        contentLayout = findViewById(R.id.contentLayout);
        titleText = findViewById(R.id.titleText);

        findViewById(R.id.activitiesButton).setOnClickListener(v -> loadActivities());
        findViewById(R.id.createButton).setOnClickListener(v -> showCreateActivityDialog());
        findViewById(R.id.paysButton).setOnClickListener(v -> loadPays());
        findViewById(R.id.pendingButton).setOnClickListener(v -> loadPendingSettlements());
        findViewById(R.id.reportsButton).setOnClickListener(v -> loadReports());
        findViewById(R.id.profileButton).setOnClickListener(v -> loadProfile());
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
        if (adminMode) {
            loadAdminPanel();
        } else {
            loadWarningsThenActivities();
        }
    }

    private void loadWarningsThenActivities() {
        titleText.setText("首页");
        setLoading();
        ApiClient.get().warnings().enqueue(new Callback<ApiResponse<List<Report>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Report>>> call, Response<ApiResponse<List<Report>>> response) {
                ApiResponse<List<Report>> body = response.body();
                clear();
                if (body != null && body.isSuccess() && body.data != null && !body.data.isEmpty()) {
                    addText("警告举报");
                    for (Report report : body.data) {
                        addText("举报#" + report.reportID
                                + " 活动#" + report.accusedActivityID
                                + "\n原因：" + safe(report.reportReason)
                                + "\n处罚：" + safe(report.punishmentType)
                                + "  截止：" + safe(report.punishmentEndTime));
                    }
                    addButton("进入活动列表", v -> loadActivities());
                } else {
                    loadActivities();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Report>>> call, Throwable t) {
                loadActivities();
            }
        });
    }

    private void loadActivities() {
        titleText.setText("活动列表");
        setLoading();
        ApiClient.get().activities(activityKeyword, activityIsSettable, activityMyCreatedUnsettled).enqueue(new Callback<ApiResponse<List<GroupActivity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<GroupActivity>>> call, Response<ApiResponse<List<GroupActivity>>> response) {
                ApiResponse<List<GroupActivity>> body = response.body();
                clear();
                if (!isOk(body)) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                currentActivities = body.data == null ? new ArrayList<>() : body.data;
                addActivityFilterControls();
                if (body.data == null || body.data.isEmpty()) {
                    addText("暂无活动");
                    return;
                }
                for (GroupActivity activity : body.data) {
                    String text = activity.titleLine()
                            + "\n金额：" + activity.actualAmount
                            + "  已结算：" + yesNo(activity.isSettable)
                            + "\n我的确认状态：" + safe(activity.currentUserConfirmStatus);
                    addButton(text, v -> loadActivityDetail(activity.activityID));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<GroupActivity>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void addActivityFilterControls() {
        addText("筛选：关键词=" + safe(activityKeyword)
                + "  结算状态=" + settleFilterText(activityIsSettable)
                + "  我创建的未结算=" + yesNo(activityMyCreatedUnsettled));
        addButton("筛选/搜索活动", v -> showActivityFilterDialog());
        if (Boolean.TRUE.equals(activityMyCreatedUnsettled)) {
            addButton("批量结算当前可结算项目", v -> showBatchSettlementDialog());
        }
    }

    private void showActivityFilterDialog() {
        LinearLayout form = dialogForm();
        EditText keyword = input("关键词，可空");
        keyword.setText(safe(activityKeyword));
        form.addView(keyword);
        new AlertDialog.Builder(this)
                .setTitle("活动筛选")
                .setView(form)
                .setItems(new String[]{"全部", "未结算", "已结算", "我创建未结算"}, (dialog, which) -> {
                    activityKeyword = text(keyword);
                    activityIsSettable = which == 1 || which == 3 ? false : (which == 2 ? true : null);
                    activityMyCreatedUnsettled = which == 3 ? true : null;
                    loadActivities();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showBatchSettlementDialog() {
        List<Integer> ids = new ArrayList<>();
        for (GroupActivity activity : currentActivities) {
            if (Boolean.TRUE.equals(activity.canSettle)) {
                ids.add(activity.activityID);
            }
        }
        if (ids.isEmpty()) {
            clear();
            addActivityFilterControls();
            addText("当前列表没有可批量结算的项目");
            return;
        }
        EditText description = input("批量结算说明");
        new AlertDialog.Builder(this)
                .setTitle("批量结算 " + ids.size() + " 个项目")
                .setView(description)
                .setPositiveButton("提交", (dialog, which) ->
                        simpleAction(ApiClient.get().batchSettlement(ids, text(description)), "批量结算已发起", this::loadActivities))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadActivityDetail(Integer activityID) {
        titleText.setText("活动详情");
        setLoading();
        ApiClient.get().activityDetail(activityID).enqueue(new Callback<ApiResponse<ActivityDetail>>() {
            @Override
            public void onResponse(Call<ApiResponse<ActivityDetail>> call, Response<ApiResponse<ActivityDetail>> response) {
                ApiResponse<ActivityDetail> body = response.body();
                clear();
                if (!isOk(body) || body.data == null || body.data.activity == null) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                renderActivityDetail(body.data);
            }

            @Override
            public void onFailure(Call<ApiResponse<ActivityDetail>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void renderActivityDetail(ActivityDetail detail) {
        GroupActivity activity = detail.activity;
        addText(activity.titleLine());
        if (activity.description != null && !activity.description.isEmpty()) {
            addText("说明：" + activity.description);
        }
        addText("实际金额：" + activity.actualAmount);
        addText("创建者：" + safe(activity.creatorName) + "  成员数：" + (detail.members == null ? 0 : detail.members.size()));
        addText("确认已发起：" + yesNo(activity.confirmInitiated));
        addText("我的确认状态：" + safe(activity.currentUserConfirmStatus));
        addText("结算状态：" + yesNo(activity.isSettable));
        if (activity.settleUnavailableReason != null && !activity.settleUnavailableReason.isEmpty()) {
            addText("当前不可结算原因：" + activity.settleUnavailableReason);
        }

        addText("成员");
        if (detail.members == null || detail.members.isEmpty()) {
            addText("暂无成员");
        } else {
            for (ActivityMember member : detail.members) {
                addText("#" + member.memberID + " " + safe(member.memberName)
                        + "  " + safe(member.role)
                        + "  " + safe(member.confirmAttendStatus));
                if (Boolean.TRUE.equals(detail.isCreator) && !ROLE_CREATOR.equals(member.role)) {
                    if (STATUS_QUIT_APPLY.equals(member.confirmAttendStatus)) {
                        addButton("同意退出 #" + member.memberID,
                                v -> simpleAction(ApiClient.get().handleQuit(activity.activityID, member.memberID, true),
                                        "已同意退出", () -> loadActivityDetail(activity.activityID)));
                        addButton("拒绝退出 #" + member.memberID,
                                v -> simpleAction(ApiClient.get().handleQuit(activity.activityID, member.memberID, false),
                                        "已拒绝退出", () -> loadActivityDetail(activity.activityID)));
                    }
                    addButton("移除成员 #" + member.memberID,
                            v -> showRemoveMemberDialog(activity.activityID, member.memberID));
                }
            }
        }

        if (detail.collect != null) {
            addText("收款：" + detail.collect.amount + "  状态：" + safe(detail.collect.collectStatus)
                    + "\n说明：" + safe(detail.collect.description));
        }
        if (detail.pays != null && !detail.pays.isEmpty()) {
            addText("付款记录");
            for (Pay pay : detail.pays) {
                addText("付款#" + pay.payID + " 用户#" + pay.userID
                        + " 金额：" + pay.payAmount + " 状态：" + safe(pay.payStatus));
            }
        }

        if (Boolean.TRUE.equals(detail.isCreator)) {
            addText("创建者操作");
            if (!Boolean.TRUE.equals(activity.isSettable)) {
                addButton("编辑项目", v -> showEditActivityDialog(activity));
                addButton("删除未结算项目", v -> confirmDeleteActivity(activity.activityID));
                addButton("添加成员", v -> showAddMemberDialog(activity.activityID));
            }
            if (!Boolean.TRUE.equals(activity.confirmInitiated) && !Boolean.TRUE.equals(activity.isSettable)) {
                addButton("发起项目确认", v -> simpleAction(ApiClient.get().initConfirm(activity.activityID), "已发起确认",
                        () -> loadActivityDetail(activity.activityID)));
            }
            if (Boolean.TRUE.equals(activity.canSettle) && !Boolean.TRUE.equals(activity.isSettable)) {
                addButton("发起结算", v -> showSettlementDialog(activity.activityID));
            } else if (!Boolean.TRUE.equals(activity.isSettable)) {
                addText("暂不可结算：" + safe(activity.settleUnavailableReason));
            }
        } else {
            addText("参与者操作");
            if (Boolean.TRUE.equals(activity.confirmInitiated)
                    && STATUS_UNCONFIRMED.equals(activity.currentUserConfirmStatus)) {
                addButton("确认参加", v -> simpleAction(ApiClient.get().confirm(activity.activityID), "已确认",
                        () -> loadActivityDetail(activity.activityID)));
                addButton("申请退出", v -> simpleAction(ApiClient.get().applyQuit(activity.activityID), "已提交退出申请",
                        () -> loadActivityDetail(activity.activityID)));
            } else if (STATUS_CONFIRMED.equals(activity.currentUserConfirmStatus)) {
                addText("你已确认参加");
            } else if (STATUS_QUIT_APPLY.equals(activity.currentUserConfirmStatus)) {
                addText("退出申请待创建者处理");
            } else if (!Boolean.TRUE.equals(activity.confirmInitiated)) {
                addText("创建者尚未发起项目确认");
            }
        }
        addButton("发布举报", v -> showReportDialog(activity.activityID));
        addButton("返回活动列表", v -> loadActivities());
    }

    private void showEditActivityDialog(GroupActivity activity) {
        LinearLayout form = dialogForm();
        EditText name = input("活动名称");
        EditText amount = input("实际金额");
        EditText maxMember = input("最大人数，可空");
        EditText description = input("说明");
        name.setText(safe(activity.activityName));
        amount.setText(activity.actualAmount == null ? "" : String.valueOf(activity.actualAmount));
        maxMember.setText(activity.maxMember == null ? "" : String.valueOf(activity.maxMember));
        description.setText(safe(activity.description));
        form.addView(name);
        form.addView(amount);
        form.addView(maxMember);
        form.addView(description);
        new AlertDialog.Builder(this)
                .setTitle("编辑项目")
                .setView(form)
                .setPositiveButton("保存", (dialog, which) -> updateActivity(activity.activityID, name, amount, maxMember, description))
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateActivity(Integer activityID, EditText name, EditText amount, EditText maxMember, EditText description) {
        Integer max = null;
        BigDecimal money;
        try {
            if (!text(maxMember).isEmpty()) {
                max = Integer.parseInt(text(maxMember));
            }
            money = new BigDecimal(text(amount));
        } catch (NumberFormatException e) {
            addText("请输入合法的金额和最大人数");
            return;
        }
        setLoading();
        ApiClient.get().updateActivity(activityID, text(name), text(description), money, max)
                .enqueue(new Callback<ApiResponse<GroupActivity>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<GroupActivity>> call, Response<ApiResponse<GroupActivity>> response) {
                        ApiResponse<GroupActivity> body = response.body();
                        if (body != null && body.isSuccess()) {
                            loadActivityDetail(activityID);
                        } else {
                            clear();
                            addText(body == null ? "保存失败，HTTP " + response.code() : body.message);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<GroupActivity>> call, Throwable t) {
                        showError(t);
                    }
                });
    }

    private void confirmDeleteActivity(Integer activityID) {
        new AlertDialog.Builder(this)
                .setTitle("删除项目")
                .setMessage("只能删除未结算项目，确认删除？")
                .setPositiveButton("删除", (dialog, which) ->
                        simpleAction(ApiClient.get().deleteActivity(activityID), "项目已删除", this::loadActivities))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCreateActivityDialog() {
        LinearLayout form = dialogForm();
        EditText name = input("活动名称");
        EditText amount = input("实际金额");
        EditText maxMember = input("最大人数，可空");
        EditText inviteCodes = input("成员邀请码，多个用逗号分隔，可空");
        EditText description = input("说明");
        form.addView(name);
        form.addView(amount);
        form.addView(maxMember);
        form.addView(inviteCodes);
        form.addView(description);
        new AlertDialog.Builder(this)
                .setTitle("创建活动")
                .setView(form)
                .setPositiveButton("创建", (dialog, which) -> createActivity(name, amount, maxMember, inviteCodes, description))
                .setNegativeButton("取消", null)
                .show();
    }

    private void createActivity(EditText name, EditText amount, EditText maxMember, EditText inviteCodes, EditText description) {
        Integer max = null;
        BigDecimal money;
        try {
            if (!text(maxMember).isEmpty()) {
                max = Integer.parseInt(text(maxMember));
            }
            money = new BigDecimal(text(amount));
        } catch (NumberFormatException e) {
            addText("请输入合法的金额和最大人数");
            return;
        }
        if (text(name).isEmpty() || money.compareTo(BigDecimal.ZERO) <= 0) {
            addText("活动名称不能为空，实际金额必须大于0");
            return;
        }
        setLoading();
        ApiClient.get().createActivity(text(name), text(description), money, max, parseInviteCodes(text(inviteCodes)))
                .enqueue(new Callback<ApiResponse<GroupActivity>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<GroupActivity>> call, Response<ApiResponse<GroupActivity>> response) {
                        ApiResponse<GroupActivity> body = response.body();
                        if (body != null && body.isSuccess() && body.data != null) {
                            loadActivityDetail(body.data.activityID);
                        } else {
                            clear();
                            addText(body == null ? "创建失败，HTTP " + response.code() : body.message);
                            addText("如果提示需要收款码，请先在“我的”中上传收款码。");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<GroupActivity>> call, Throwable t) {
                        showError(t);
                    }
                });
    }

    private void showAddMemberDialog(Integer activityID) {
        EditText inviteCode = input("成员邀请码");
        new AlertDialog.Builder(this)
                .setTitle("添加成员")
                .setView(inviteCode)
                .setPositiveButton("添加", (dialog, which) ->
                        simpleAction(ApiClient.get().addMember(activityID, text(inviteCode)), "成员已添加",
                                () -> loadActivityDetail(activityID)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRemoveMemberDialog(Integer activityID, Integer memberID) {
        EditText reason = input("移除原因，可空");
        new AlertDialog.Builder(this)
                .setTitle("移除成员 #" + memberID)
                .setView(reason)
                .setPositiveButton("移除", (dialog, which) ->
                        simpleAction(ApiClient.get().removeMember(activityID, memberID, text(reason)), "成员已移除",
                                () -> loadActivityDetail(activityID)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showSettlementDialog(Integer activityID) {
        EditText description = input("结算说明");
        new AlertDialog.Builder(this)
                .setTitle("发起结算")
                .setView(description)
                .setPositiveButton("提交", (dialog, which) ->
                        simpleAction(ApiClient.get().initSettlement(activityID, text(description)), "已发起结算",
                                () -> loadActivityDetail(activityID)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showReportDialog(Integer activityID) {
        LinearLayout form = dialogForm();
        EditText accused = input("被举报用户ID/手机号/邮箱");
        EditText reason = input("举报原因");
        form.addView(accused);
        form.addView(reason);
        new AlertDialog.Builder(this)
                .setTitle("发布举报")
                .setView(form)
                .setPositiveButton("提交", (dialog, which) ->
                        simpleAction(ApiClient.get().publishReport(activityID, text(accused), text(reason)), "举报已提交",
                                () -> loadActivityDetail(activityID)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadPays() {
        titleText.setText("我的付款");
        setLoading();
        ApiClient.get().myPays().enqueue(new Callback<ApiResponse<List<Pay>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Pay>>> call, Response<ApiResponse<List<Pay>>> response) {
                ApiResponse<List<Pay>> body = response.body();
                clear();
                if (!isOk(body)) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                if (body.data == null || body.data.isEmpty()) {
                    addText("暂无付款记录");
                    return;
                }
                for (Pay pay : body.data) {
                    addButton("付款#" + pay.payID + " 活动#" + pay.activityID
                                    + "\n金额：" + pay.payAmount + " 状态：" + safe(pay.payStatus),
                            v -> loadPaymentDetail(pay.payID));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Pay>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void loadPendingSettlements() {
        titleText.setText("待结算项目");
        setLoading();
        ApiClient.get().myPays().enqueue(new Callback<ApiResponse<List<Pay>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Pay>>> call, Response<ApiResponse<List<Pay>>> response) {
                ApiResponse<List<Pay>> body = response.body();
                clear();
                if (!isOk(body)) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                addText("待支付");
                int unpaidCount = 0;
                if (body.data != null) {
                    for (Pay pay : body.data) {
                        if (STATUS_UNPAID.equals(pay.payStatus)) {
                            unpaidCount++;
                            addButton("付款#" + pay.payID + " 活动#" + pay.activityID
                                            + "\n金额：" + pay.payAmount,
                                    v -> loadPaymentDetail(pay.payID));
                        }
                    }
                }
                if (unpaidCount == 0) {
                    addText("暂无待支付记录");
                }
                addText("待收款");
                addText("当前 Android 端缺少后端个人收款统计接口，待收款仍需从活动详情的付款记录查看。");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Pay>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void loadPaymentDetail(Integer payID) {
        titleText.setText("付款详情");
        setLoading();
        ApiClient.get().payDetail(payID).enqueue(new Callback<ApiResponse<PaymentDetail>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentDetail>> call, Response<ApiResponse<PaymentDetail>> response) {
                ApiResponse<PaymentDetail> body = response.body();
                clear();
                if (!isOk(body) || body.data == null || body.data.pay == null) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                PaymentDetail detail = body.data;
                Pay pay = detail.pay;
                addText("付款#" + pay.payID + " 活动#" + pay.activityID);
                if (detail.activity != null) {
                    addText("活动：" + detail.activity.titleLine());
                }
                addText("金额：" + pay.payAmount + " 状态：" + safe(pay.payStatus));
                if (pay.payMethod != null && !pay.payMethod.isEmpty()) {
                    addText("支付方式：" + pay.payMethod);
                }
                if (detail.creator != null) {
                    addText("收款方：" + detail.creator.displayName());
                    if (detail.creator.paymentQrCodePath != null && !detail.creator.paymentQrCodePath.isEmpty()) {
                        addText("收款码：");
                        addRemoteImage(detail.creator.paymentQrCodePath);
                    }
                }
                if (pay.payRemark != null && !pay.payRemark.isEmpty()) {
                    addText("备注：" + pay.payRemark);
                }
                if (pay.paymentProofPath != null && !pay.paymentProofPath.isEmpty()) {
                    addText("付款凭证：");
                    addRemoteImage(pay.paymentProofPath);
                }
                if (STATUS_UNPAID.equals(pay.payStatus)) {
                    addButton("选择支付方式并完成付款", v -> showPaymentMethodDialog(pay.payID));
                }
                addButton("返回我的付款", v -> loadPays());
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentDetail>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void showPaymentMethodDialog(Integer payID) {
        String[] methods = {PAY_METHOD_QR_CODE, PAY_METHOD_THIRD_PART};
        new AlertDialog.Builder(this)
                .setTitle("选择支付方式")
                .setItems(methods, (dialog, which) -> showPaymentDialog(payID, methods[which]))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showPaymentDialog(Integer payID, String payMethod) {
        EditText remark = input("付款备注，可空");
        new AlertDialog.Builder(this)
                .setTitle("完成付款")
                .setMessage("支付方式：" + payMethod + "\n请选择付款凭证图片，支持 jpg/jpeg/png。")
                .setView(remark)
                .setPositiveButton("选择图片", (dialog, which) -> {
                    pendingPayId = payID;
                    pendingPayRemark = text(remark);
                    pendingPayMethod = payMethod;
                    openImagePicker(REQUEST_PAYMENT_PROOF);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadReports() {
        titleText.setText("我的举报");
        setLoading();
        ApiClient.get().myReports().enqueue(new Callback<ApiResponse<List<Report>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Report>>> call, Response<ApiResponse<List<Report>>> response) {
                ApiResponse<List<Report>> body = response.body();
                clear();
                if (!isOk(body)) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                if (body.data == null || body.data.isEmpty()) {
                    addText("暂无举报记录");
                    return;
                }
                for (Report report : body.data) {
                    addText("举报#" + report.reportID + " 活动#" + report.accusedActivityID
                            + "\n原因：" + safe(report.reportReason)
                            + "\n状态：" + safe(report.reportProcessStatus));
                    if (STATUS_PENDING_REVIEW.equals(report.reportProcessStatus)) {
                        addButton("撤销举报 #" + report.reportID, v -> confirmCancelReport(report.reportID));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Report>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void confirmCancelReport(Integer reportID) {
        new AlertDialog.Builder(this)
                .setTitle("撤销举报")
                .setMessage("确认撤销待审核举报？")
                .setPositiveButton("撤销", (dialog, which) ->
                        simpleAction(ApiClient.get().cancelReport(reportID), "举报已撤销", this::loadReports))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadProfile() {
        titleText.setText("我的信息");
        setLoading();
        ApiClient.get().me().enqueue(new Callback<ApiResponse<OrdinaryUser>>() {
            @Override
            public void onResponse(Call<ApiResponse<OrdinaryUser>> call, Response<ApiResponse<OrdinaryUser>> response) {
                ApiResponse<OrdinaryUser> body = response.body();
                clear();
                if (!isOk(body) || body.data == null) {
                    addText(body == null ? "加载失败" : body.message);
                    return;
                }
                renderProfile(body.data);
            }

            @Override
            public void onFailure(Call<ApiResponse<OrdinaryUser>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void renderProfile(OrdinaryUser user) {
        addText("用户ID：" + user.userID);
        addText("用户名：" + user.displayName());
        addText("手机号：" + safe(user.phoneNumber));
        addText("邮箱：" + safe(user.userMailbox));
        addText("真实姓名：" + safe(user.realName));
        addText("性别：" + safe(user.gender));
        addText("生日：" + safe(user.birthday));
        addText("邀请码：" + safe(user.inviteCode));
        if (user.totalIncome != null || user.totalExpense != null) {
            addText("总收入：" + safe(user.totalIncome) + "  总支出：" + safe(user.totalExpense));
        } else {
            addText("个人收支统计：后端暂未返回统计字段");
        }
        addText("收款码：" + safe(user.paymentQrCodePath));
        addRemoteImage(user.paymentQrCodePath);
        addButton("编辑个人信息", v -> showEditProfileDialog(user));
        addButton("修改密码", v -> showChangePasswordDialog());
        addButton("上传/更新收款码", v -> openImagePicker(REQUEST_PAYMENT_QR_CODE));
        addButton("注销账号", v -> confirmDeleteCurrentUser());
    }

    private void showEditProfileDialog(OrdinaryUser user) {
        LinearLayout form = dialogForm();
        EditText name = input("用户名");
        EditText phone = input("手机号");
        EditText email = input("邮箱");
        EditText realName = input("真实姓名");
        EditText gender = input("性别");
        EditText birthday = input("生日，例：2000-01-01");
        name.setText(safe(user.userName));
        phone.setText(safe(user.phoneNumber));
        email.setText(safe(user.userMailbox));
        realName.setText(safe(user.realName));
        gender.setText(safe(user.gender));
        birthday.setText(safe(user.birthday));
        form.addView(name);
        form.addView(phone);
        form.addView(email);
        form.addView(realName);
        form.addView(gender);
        form.addView(birthday);
        new AlertDialog.Builder(this)
                .setTitle("编辑个人信息")
                .setView(form)
                .setPositiveButton("保存", (dialog, which) -> updateProfile(name, phone, email, realName, gender, birthday))
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateProfile(EditText name, EditText phone, EditText email, EditText realName,
                               EditText gender, EditText birthday) {
        setLoading();
        ApiClient.get().updateUser(text(name), text(phone), text(email), text(realName), text(gender), text(birthday))
                .enqueue(new Callback<ApiResponse<OrdinaryUser>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<OrdinaryUser>> call, Response<ApiResponse<OrdinaryUser>> response) {
                        ApiResponse<OrdinaryUser> body = response.body();
                        clear();
                        if (body != null && body.isSuccess() && body.data != null) {
                            addText("个人信息已更新");
                            renderProfile(body.data);
                        } else {
                            handleApiFailure(body, "保存失败");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<OrdinaryUser>> call, Throwable t) {
                        showError(t);
                    }
                });
    }

    private void showChangePasswordDialog() {
        LinearLayout form = dialogForm();
        EditText oldPassword = input("旧密码");
        oldPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newPassword = input("新密码");
        newPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmPassword = input("确认新密码");
        confirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(oldPassword);
        form.addView(newPassword);
        form.addView(confirmPassword);
        new AlertDialog.Builder(this)
                .setTitle("修改密码")
                .setView(form)
                .setPositiveButton("提交", (dialog, which) -> changePassword(oldPassword, newPassword, confirmPassword))
                .setNegativeButton("取消", null)
                .show();
    }

    private void changePassword(EditText oldPassword, EditText newPassword, EditText confirmPassword) {
        simpleAction(ApiClient.get().changePassword(text(oldPassword), text(newPassword), text(confirmPassword)),
                "密码已修改，请重新登录", this::goLogin);
    }

    private void confirmDeleteCurrentUser() {
        new AlertDialog.Builder(this)
                .setTitle("注销账号")
                .setMessage("确认注销当前账号？该操作会删除账号并退出登录。")
                .setPositiveButton("注销", (dialog, which) ->
                        simpleAction(ApiClient.get().deleteCurrentUser(), "账号已注销", this::goLogin))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadAdminPanel() {
        titleText.setText("管理员");
        clear();
        addButton("管理用户", v -> loadAdminUsers());
        addButton("查询项目", v -> loadAdminActivities());
        addButton("审核举报", v -> loadAdminReports());
        addButton("退出登录", v -> logout());
    }

    private void loadAdminUsers() {
        titleText.setText("管理用户");
        setLoading();
        ApiClient.get().adminUsers().enqueue(new Callback<ApiResponse<List<OrdinaryUser>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OrdinaryUser>>> call, Response<ApiResponse<List<OrdinaryUser>>> response) {
                ApiResponse<List<OrdinaryUser>> body = response.body();
                clear();
                if (!isOk(body)) {
                    handleApiFailure(body, "加载用户失败");
                    return;
                }
                if (body.data == null || body.data.isEmpty()) {
                    addText("暂无用户");
                } else {
                    addAdminUserSearchControls();
                    int matchedCount = 0;
                    for (OrdinaryUser user : body.data) {
                        if (!matchesUserKeyword(user, adminUserKeyword)) {
                            continue;
                        }
                        matchedCount++;
                        addText("#" + user.userID + " " + user.displayName()
                                + "\n手机号：" + safe(user.phoneNumber)
                                + "  邮箱：" + safe(user.userMailbox));
                        addButton("删除用户 #" + user.userID, v -> confirmAdminDeleteUser(user.userID));
                    }
                    if (matchedCount == 0) {
                        addText("没有匹配的用户");
                    }
                }
                addButton("返回管理员面板", v -> loadAdminPanel());
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OrdinaryUser>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void addAdminUserSearchControls() {
        addText("搜索关键词：" + safe(adminUserKeyword));
        addButton("搜索用户", v -> showAdminUserSearchDialog());
        if (adminUserKeyword != null && !adminUserKeyword.isEmpty()) {
            addButton("清除搜索", v -> {
                adminUserKeyword = null;
                loadAdminUsers();
            });
        }
    }

    private void showAdminUserSearchDialog() {
        EditText keyword = input("用户名/手机号/邮箱/用户ID");
        keyword.setText(safe(adminUserKeyword));
        new AlertDialog.Builder(this)
                .setTitle("搜索用户")
                .setView(keyword)
                .setPositiveButton("搜索", (dialog, which) -> {
                    adminUserKeyword = text(keyword);
                    loadAdminUsers();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean matchesUserKeyword(OrdinaryUser user, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String lower = keyword.trim().toLowerCase();
        return containsLower(user.userID == null ? "" : String.valueOf(user.userID), lower)
                || containsLower(user.userName, lower)
                || containsLower(user.realName, lower)
                || containsLower(user.phoneNumber, lower)
                || containsLower(user.userMailbox, lower);
    }

    private void loadAdminActivities() {
        titleText.setText("查询项目");
        setLoading();
        ApiClient.get().activities(null, null, null).enqueue(new Callback<ApiResponse<List<GroupActivity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<GroupActivity>>> call, Response<ApiResponse<List<GroupActivity>>> response) {
                ApiResponse<List<GroupActivity>> body = response.body();
                clear();
                if (!isOk(body)) {
                    handleApiFailure(body, "加载项目失败");
                    return;
                }
                if (body.data == null || body.data.isEmpty()) {
                    addText("暂无项目");
                } else {
                    for (GroupActivity activity : body.data) {
                        addText(activity.titleLine()
                                + "\n创建者：" + safe(activity.creatorName)
                                + "  金额：" + activity.actualAmount
                                + "  已结算：" + yesNo(activity.isSettable));
                    }
                }
                addButton("返回管理员面板", v -> loadAdminPanel());
            }

            @Override
            public void onFailure(Call<ApiResponse<List<GroupActivity>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void confirmAdminDeleteUser(Integer userID) {
        new AlertDialog.Builder(this)
                .setTitle("删除用户")
                .setMessage("确认删除用户 #" + userID + "？")
                .setPositiveButton("删除", (dialog, which) ->
                        simpleAction(ApiClient.get().adminDeleteUser(userID), "用户已删除", this::loadAdminUsers))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadAdminReports() {
        titleText.setText("审核举报");
        setLoading();
        ApiClient.get().adminReports(null, null).enqueue(new Callback<ApiResponse<List<Report>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Report>>> call, Response<ApiResponse<List<Report>>> response) {
                ApiResponse<List<Report>> body = response.body();
                clear();
                if (!isOk(body)) {
                    handleApiFailure(body, "加载举报失败");
                    return;
                }
                if (body.data == null || body.data.isEmpty()) {
                    addText("暂无举报");
                } else {
                    for (Report report : body.data) {
                        addText("举报#" + report.reportID
                                + " 活动#" + report.accusedActivityID
                                + " 被举报用户#" + report.accusedUserID
                                + "\n原因：" + safe(report.reportReason)
                                + "\n状态：" + safe(report.reportProcessStatus)
                                + "  处罚：" + safe(report.punishmentType));
                        if (STATUS_PENDING_REVIEW.equals(report.reportProcessStatus)) {
                            addButton("通过举报 #" + report.reportID,
                                    v -> showAuditDialog(report.reportID));
                            addButton("驳回举报 #" + report.reportID,
                                    v -> auditReport(report.reportID, "已驳回", ""));
                        }
                    }
                }
                addButton("返回管理员面板", v -> loadAdminPanel());
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Report>>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void showAuditDialog(Integer reportID) {
        String[] punishmentTypes = {"警告", "封号1天", "封号7天", "封号30天", "永久封号"};
        new AlertDialog.Builder(this)
                .setTitle("选择处罚类型")
                .setItems(punishmentTypes, (dialog, which) -> {
                    String punishment = punishmentTypes[which];
                    auditReport(reportID, "已通过", punishment);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void auditReport(Integer reportID, String status, String punishmentType) {
        simpleAction(ApiClient.get().adminAuditReport(reportID, status, punishmentType),
                "举报已审核", this::loadAdminReports);
    }

    private void logout() {
        ApiClient.get().logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                goLogin();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                goLogin();
            }
        });
    }

    private void simpleAction(Call<ApiResponse<Void>> call, String okText, Runnable onSuccess) {
        setLoading();
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                ApiResponse<Void> body = response.body();
                if (body != null && body.isSuccess()) {
                    addText(okText);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    clear();
                    handleApiFailure(body, "操作失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                showError(t);
            }
        });
    }

    private void handleApiFailure(ApiResponse<?> body, String fallback) {
        if (body != null && body.code == 401) {
            addText("登录已过期，请重新登录");
            goLogin();
            return;
        }
        addText(body == null ? fallback : body.message);
    }

    private void goLogin() {
        ApiClient.clearSession();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void uploadPaymentQrCode(Uri uri) {
        try {
            MultipartBody.Part part = imagePart("paymentQrCode", uri);
            setLoading();
            ApiClient.get().uploadPaymentQrCode(part).enqueue(new Callback<ApiResponse<OrdinaryUser>>() {
                @Override
                public void onResponse(Call<ApiResponse<OrdinaryUser>> call, Response<ApiResponse<OrdinaryUser>> response) {
                    ApiResponse<OrdinaryUser> body = response.body();
                    clear();
                    if (body != null && body.isSuccess() && body.data != null) {
                        addText("收款码已上传");
                        renderProfile(body.data);
                    } else {
                        addText(body == null ? "上传失败" : body.message);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<OrdinaryUser>> call, Throwable t) {
                    showError(t);
                }
            });
        } catch (Exception e) {
            clear();
            addText("读取图片失败：" + e.getMessage());
        }
    }

    private void finishPayment(Uri uri) {
        if (pendingPayId == null) {
            addText("付款记录不存在");
            return;
        }
        try {
            MultipartBody.Part part = imagePart("paymentProof", uri);
            RequestBody payID = textBody(String.valueOf(pendingPayId));
            RequestBody remark = textBody(pendingPayRemark == null ? "" : pendingPayRemark);
            RequestBody payMethod = textBody(pendingPayMethod == null ? PAY_METHOD_QR_CODE : pendingPayMethod);
            Integer payIdToRefresh = pendingPayId;
            setLoading();
            ApiClient.get().finishPayment(payID, remark, payMethod, part).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    ApiResponse<Void> body = response.body();
                    if (body != null && body.isSuccess()) {
                        loadPaymentDetail(payIdToRefresh);
                    } else {
                        clear();
                        addText(body == null ? "付款失败" : body.message);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    showError(t);
                }
            });
        } catch (Exception e) {
            clear();
            addText("读取图片失败：" + e.getMessage());
        } finally {
            pendingPayId = null;
            pendingPayRemark = null;
            pendingPayMethod = null;
        }
    }

    private void openImagePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_PAYMENT_QR_CODE) {
            uploadPaymentQrCode(uri);
        } else if (requestCode == REQUEST_PAYMENT_PROOF) {
            finishPayment(uri);
        }
    }

    private MultipartBody.Part imagePart(String partName, Uri uri) throws Exception {
        File file = copyUriToCache(uri);
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "image/*";
        }
        RequestBody requestBody = RequestBody.create(file, MediaType.parse(mimeType));
        return MultipartBody.Part.createFormData(partName, file.getName(), requestBody);
    }

    private File copyUriToCache(Uri uri) throws Exception {
        String filename = queryFileName(uri);
        String extension = extensionFromName(filename);
        if (extension.isEmpty()) {
            extension = extensionFromMime(getContentResolver().getType(uri));
        }
        if (extension.isEmpty()) {
            extension = "jpg";
        }
        File file = File.createTempFile("upload_", "." + extension, getCacheDir());
        try (InputStream input = getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(file)) {
            if (input == null) {
                throw new IllegalArgumentException("无法打开图片");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        return file;
    }

    private String queryFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        }
        return "";
    }

    private String extensionFromName(String filename) {
        int dotIndex = filename == null ? -1 : filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }

    private String extensionFromMime(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return extension == null ? "" : extension;
    }

    private RequestBody textBody(String value) {
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private List<String> parseInviteCodes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] parts = raw.trim().split("[,，\\s]+");
        List<String> codes = new ArrayList<>();
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                codes.add(part.trim());
            }
        }
        return codes.isEmpty() ? null : codes;
    }

    private void setLoading() {
        clear();
        addText("加载中...");
    }

    private void clear() {
        contentLayout.removeAllViews();
    }

    private void addText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(getResources().getColor(R.color.text_primary));
        view.setPadding(8, 10, 8, 10);
        contentLayout.addView(view);
    }

    private void addButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        contentLayout.addView(button);
    }

    private void addRemoteImage(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxHeight(600);
        imageView.setPadding(8, 8, 8, 8);
        contentLayout.addView(imageView);
        String url = ApiConfig.DEFAULT_BASE_URL + (path.startsWith("/") ? path.substring(1) : path);
        new Thread(() -> {
            try (InputStream input = new URL(url).openStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                runOnUiThread(() -> {
                    if (bitmap == null) {
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                    } else {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                    imageView.setContentDescription("图片暂不可用");
                });
            }
        }).start();
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 8, 24, 0);
        return form;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        return editText;
    }

    private boolean isOk(ApiResponse<?> body) {
        return body != null && body.isSuccess();
    }

    private void showError(Throwable t) {
        clear();
        addText("网络错误：" + t.getMessage());
    }

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean containsLower(String value, String lowerKeyword) {
        return value != null && value.toLowerCase().contains(lowerKeyword);
    }

    private String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }

    private String settleFilterText(Boolean value) {
        if (value == null) {
            return "全部";
        }
        return Boolean.TRUE.equals(value) ? "已结算" : "未结算";
    }
}
