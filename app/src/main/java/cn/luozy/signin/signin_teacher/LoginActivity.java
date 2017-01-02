package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.Map;

import static cn.luozy.signin.signin_teacher.Utility.postRequest;

/*
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private final String loginURL = "https://signin.luozy.cn/api/teacher/login";

    private final String json_teacher_id = "user_id";
    private final String json_teacher_token = "token";
    private final String json_teacher_name = "name";
    private final String json_password = "password";
    private final String json_error_message = "errmsg";

    private String teacher_id;
    private String teacher_token;
    private String teacher_name;

    private EditText editTextID;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextID = (EditText) findViewById(R.id.editTextID);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);

        Button buttonSignIn = (Button) findViewById(R.id.buttonSignIn);
        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    public void run() {
                        attemptLogin();
                    }
                }.start();
            }
        });

        TextView textViewRegister = (TextView) findViewById(R.id.textViewRegister);
        textViewRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    String resp = msg.getData().getString("resp");
                    try {
                        JSONTokener jsonTokener = new JSONTokener(resp);
                        JSONObject jsonObject = (JSONObject) jsonTokener.nextValue();

                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(LoginActivity.this, jsonObject.getString(json_error_message));
                        }

                        if (jsonObject.has(json_teacher_token)) {
                            teacher_token = jsonObject.getString(json_teacher_token);
                            teacher_name = jsonObject.getString(json_teacher_name);
                            Log.d("DDD", teacher_token);
                            SharedPreferences sharedPref = getSharedPreferences(
                                    getString(R.string.preference_login),
                                    Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.clear();
                            editor.putString(getString(R.string.preference_id_key), teacher_id)
                                    .putString(getString(R.string.preference_token_key), teacher_token)
                                    .putString(getString(R.string.preference_name_key), teacher_name)
                                    .commit();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    Utility.showTip(LoginActivity.this, "网络连接异常");
                    break;
            }
        }
    };

    private void attemptLogin() {
        Map<String, String> params = new HashMap<>();
        String id_str = editTextID.getText().toString();
        String password_str = editTextPassword.getText().toString();
        if (id_str.length() == 0) {
            Utility.showTip(this, "请输入教工号");
        } else if (password_str.length() == 0) {
            Utility.showTip(this, "请输入密码");
        } else {
            teacher_id = editTextID.getText().toString();
            params.put(json_teacher_id, editTextID.getText().toString());
            params.put(json_password, editTextPassword.getText().toString());
            String resp = postRequest(loginURL, params);
            if (resp != null) {
                Bundle bundle = new Bundle();
                bundle.putString("resp", resp);
                Message message = new Message();
                message.setData(bundle);
                message.what = 0;
                handler.sendMessage(message);
            }
        }
    }
}

