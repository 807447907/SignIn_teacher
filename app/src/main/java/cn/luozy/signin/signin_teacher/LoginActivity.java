package cn.luozy.signin.signin_teacher;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    private final String loginURL = "https://signin.luozy.cn/api/teacher/login";
    private String teacher_id;
    private String teacher_token;

    private Toast mToast;
    private EditText editTextID;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mToast = Toast.makeText(LoginActivity.this, "", Toast.LENGTH_SHORT);

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

                        if (jsonObject.getInt("status") == 0) {
                            showTip(jsonObject.getString("msg"));
                            teacher_token = jsonObject.getString("teacher_token");
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("teacher_id", teacher_id);
                            intent.putExtra("teacher_token", teacher_token);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            JSONObject errors = jsonObject.getJSONObject("errors");
                            boolean canFocus = true;
                            if (errors.has("teacher_id")) {
                                editTextID.setError(errors.getJSONArray("teacher_id").getString(0));
                                if (canFocus) {
                                    editTextID.requestFocus();
                                    canFocus = false;
                                }
                            }
                            if (errors.has("teacher_password")) {
                                editTextPassword.setError(errors.getJSONArray("teacher_password").getString(0));
                                if (canFocus) {
                                    editTextPassword.requestFocus();
                                    canFocus = false;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void attemptLogin() {
        teacher_id = editTextID.getText().toString();

        Map<String, String> params = new HashMap<>();
        params.put("teacher_id", editTextID.getText().toString());
        params.put("teacher_password", editTextPassword.getText().toString());
        String resp = postRequest(loginURL, params);
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 0;
            handler.sendMessage(message);
        }
    }

    public static String buildParam(Map<String, String> params) {
        String res = "";
        try {
            Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
            Map.Entry<String, String> entry;
            if (it.hasNext()) {
                entry = it.next();
                res += entry.getKey() + "=" + java.net.URLEncoder.encode(entry.getValue(), "UTF-8");
            }
            while (it.hasNext()) {
                entry = it.next();
                res += "&" + entry.getKey() + "=" + java.net.URLEncoder.encode(entry.getValue(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static String postRequest(String path, Map<String, String> params) {
        URL url;
        String resp = "";
        try {
            url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            PrintWriter printWriter = new PrintWriter(conn.getOutputStream());
            printWriter.write(buildParam(params));
            printWriter.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;

            while ((line = br.readLine()) != null) {
                resp += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }
}
