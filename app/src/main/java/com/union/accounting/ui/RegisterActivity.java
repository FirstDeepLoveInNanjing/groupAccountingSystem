package com.union.accounting.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.union.accounting.R;
import com.union.accounting.api.ApiClient;
import com.union.accounting.model.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends Activity {
    private EditText nameInput;
    private EditText phoneInput;
    private EditText verifyCodeInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ApiClient.init(this);
        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        verifyCodeInput = findViewById(R.id.verifyCodeInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmInput = findViewById(R.id.confirmInput);
        statusText = findViewById(R.id.statusText);
        Button sendCodeButton = findViewById(R.id.sendCodeButton);
        Button submitButton = findViewById(R.id.submitButton);
        sendCodeButton.setOnClickListener(v -> sendSmsCode());
        submitButton.setOnClickListener(v -> register());
    }

    private void sendSmsCode() {
        String phone = text(phoneInput);
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

    private void register() {
        statusText.setText("正在注册...");
        ApiClient.get().register(text(nameInput), text(passwordInput), text(confirmInput), text(phoneInput), text(emailInput), text(verifyCodeInput))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        ApiResponse<Void> body = response.body();
                        if (body != null && body.isSuccess()) {
                            statusText.setText("注册成功，请返回登录");
                        } else {
                            statusText.setText(body == null ? "注册失败" : body.message);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        statusText.setText("网络错误：" + t.getMessage());
                    }
                });
    }

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }
}
