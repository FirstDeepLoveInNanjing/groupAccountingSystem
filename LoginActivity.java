package com.union.accounting.ui;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.union.accounting.R;
import com.union.accounting.api.ApiClient;
import com.union.accounting.model.Administrator;
import com.union.accounting.model.ApiResponse;
import com.union.accounting.model.OrdinaryUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends Activity {
    private EditText accountInput;
    private EditText passwordInput;
    private EditText smsCodeInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ApiClient.init(this);
        accountInput = findViewById(R.id.accountInput);
        passwordInput = findViewById(R.id.passwordInput);
        smsCodeInput = findViewById(R.id.smsCodeInput);
        statusText = findViewById(R.id.statusText);
        Button loginButton = findViewById(R.id.loginButton);
        Button sendSmsButton = findViewById(R.id.sendSmsButton);
        Button smsLoginButton = findViewById(R.id.smsLoginButton);
        Button adminLoginButton = findViewById(R.id.adminLoginButton);
        Button registerButton = findViewById(R.id.registerButton);
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton);

        loginButton.setOnClickListener(v -> login());
        sendSmsButton.setOnClickListener(v -> sendSmsCode());
        smsLoginButton.setOnClickListener(v -> smsLogin());
        adminLoginButton.setOnClickListener(v -> adminLogin());
        registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordButton.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void login() {
        accountInput.setHint("手机号或邮箱");
        passwordInput.setHint("密码");
        statusText.setText("正在登录...");
        ApiClient.get().login(text(accountInput), text(passwordInput)).enqueue(new Callback<ApiResponse<OrdinaryUser>>() {
            @Override
            public void onResponse(Call<ApiResponse<OrdinaryUser>> call, Response<ApiResponse<OrdinaryUser>> response) {
                ApiResponse<OrdinaryUser> body = response.body();
                if (body != null && body.isSuccess()) {
                    openMain(false);
                } else {
                    statusText.setText(body == null ? "登录失败" : body.message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<OrdinaryUser>> call, Throwable t) {
                statusText.setText("网络错误：" + t.getMessage());
            }
        });
    }

    private void sendSmsCode() {
        accountInput.setHint("手机号");
        String phone = text(accountInput);
        if (phone.isEmpty()) {
            statusText.setText("请先输入手机号");
            return;
        }
        statusText.setText("正在发送验证码...");
        ApiClient.get().sendSmsLoginCode(phone).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                ApiResponse<Void> body = response.body();
                statusText.setText(body != null && body.isSuccess() ? "验证码已发送" : (body == null ? "发送失败" : body.message));
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                statusText.setText("网络错误：" + t.getMessage());
            }
        });
    }

    private void smsLogin() {
        accountInput.setHint("手机号");
        statusText.setText("正在短信登录...");
        ApiClient.get().smsLogin(text(accountInput), text(smsCodeInput)).enqueue(new Callback<ApiResponse<OrdinaryUser>>() {
            @Override
            public void onResponse(Call<ApiResponse<OrdinaryUser>> call, Response<ApiResponse<OrdinaryUser>> response) {
                ApiResponse<OrdinaryUser> body = response.body();
                if (body != null && body.isSuccess()) {
                    openMain(false);
                } else {
                    statusText.setText(body == null ? "短信登录失败" : body.message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<OrdinaryUser>> call, Throwable t) {
                statusText.setText("网络错误：" + t.getMessage());
            }
        });
    }

    private void adminLogin() {
        accountInput.setHint("管理员ID");
        passwordInput.setHint("管理员密码");
        statusText.setText("请输入管理员ID和密码");
        ApiClient.get().administratorLogin(text(accountInput), text(passwordInput)).enqueue(new Callback<ApiResponse<Administrator>>() {
            @Override
            public void onResponse(Call<ApiResponse<Administrator>> call, Response<ApiResponse<Administrator>> response) {
                ApiResponse<Administrator> body = response.body();
                if (body != null && body.isSuccess()) {
                    openMain(true);
                } else {
                    statusText.setText(body == null ? "管理员登录失败" : body.message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Administrator>> call, Throwable t) {
                statusText.setText("网络错误：" + t.getMessage());
            }
        });
    }

    private void showForgotPasswordDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 8, 24, 0);
        EditText phone = input("手机号");
        EditText verifyCode = input("短信验证码");
        EditText newPassword = input("新密码");
        newPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmPassword = input("确认新密码");
        confirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(phone);
        form.addView(verifyCode);
        form.addView(newPassword);
        form.addView(confirmPassword);
        new AlertDialog.Builder(this)
                .setTitle("找回密码")
                .setView(form)
                .setNeutralButton("发送验证码", (dialog, which) -> sendResetSmsCode(text(phone)))
                .setPositiveButton("重置密码", (dialog, which) ->
                        resetPassword(text(phone), text(verifyCode), text(newPassword), text(confirmPassword)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void sendResetSmsCode(String phone) {
        if (phone.isEmpty()) {
            statusText.setText("请先输入手机号");
            return;
        }
        statusText.setText("正在发送验证码...");
        ApiClient.get().sendSmsLoginCode(phone).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                ApiResponse<Void> body = response.body();
                statusText.setText(body != null && body.isSuccess() ? "验证码已发送" : (body == null ? "发送失败" : body.message));
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                statusText.setText("网络错误：" + t.getMessage());
            }
        });
    }

    private void resetPassword(String phone, String verifyCode, String newPassword, String confirmPassword) {
        if (phone.isEmpty() || verifyCode.isEmpty() || newPassword.isEmpty()) {
            statusText.setText("手机号、验证码和新密码不能为空");
            return;
        }
        statusText.setText("正在重置密码...");
        ApiClient.get().forgetPassword(phone, verifyCode, newPassword, confirmPassword).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                ApiResponse<Void> body = response.body();
                statusText.setText(body != null && body.isSuccess() ? "密码已重置，请登录" : (body == null ? "重置失败，HTTP " + response.code() : body.message));
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                statusText.setText("网络错误：" + t.getMessage());
            }
        });
    }

    private void openMain(boolean adminMode) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("adminMode", adminMode);
        startActivity(intent);
        finish();
    }

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        return editText;
    }
}
