package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.Map;

public class NewSignInActivity extends AppCompatActivity {
    private String teacher_id;
    private String teacher_token;

    private Toast mToast;

    private SharedPreferences mSharedPref;

    private Handler handler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sign_in);

        getSupportActionBar().setElevation(0f);

        mSharedPref = getSharedPreferences(
                getString(R.string.preference_login),
                Context.MODE_PRIVATE
        );
        teacher_id = mSharedPref.getString(getString(R.string.preference_id_key), null);
        teacher_token = mSharedPref.getString(getString(R.string.preference_token_key), null);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        Spinner spinner = (Spinner)findViewById(R.id.spinnerCourse);
        String[] items = new String[]{
                "算法分析与设计",
                "JAVA程序设计",
                "计算机系统",
                "[编辑课程]"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_signin, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_complete) {
            new Thread() {
                public void run() {
                    attemptCreate(
                            ((EditText)findViewById(R.id.editTextSignInName))
                                    .getText().toString()
                    );
                }
            }.start();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void attemptCreate(final String signIn_name) {
        Map<String, String> params = new HashMap<>();
        params.put(getString(R.string.json_sign_in_name), signIn_name);
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);

        String resp = MainActivity.postRequest(getString(R.string.url_create), params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.json_response), resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 0;
            handler.sendMessage(message);
        }
    }

    class MyHandler extends Handler {
        private NewSignInActivity mActivity;

        public MyHandler (NewSignInActivity activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String resp;
            switch (msg.what) {
                case 0:
                    resp = msg.getData().getString(getString(R.string.json_response));
                    try {
                        JSONTokener jsonTokener = new JSONTokener(resp);
                        JSONObject jsonObject = (JSONObject) jsonTokener.nextValue();
                        if (jsonObject.getInt(getString(R.string.json_status)) == 0) {
                            mActivity.showTip(jsonObject.getString(getString(R.string.json_message)));
                        } else {
                            JSONObject errors = jsonObject.getJSONObject(getString(R.string.json_errors));
                            boolean exit = false;
                            String errorMsg = null;
                            if (errors.has(getString(R.string.json_teacher_id))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_teacher_id)).getString(0);
                                exit = true;
                            } else if (errors.has(getString(R.string.json_teacher_token))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_teacher_token)).getString(0);
                                exit = true;
                            } else if (errors.has(getString(R.string.json_sign_in_name))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_sign_in_name)).getString(0);
                            }
                            mActivity.showTip(errorMsg);
                            if (exit) {
                                Intent intent = new Intent(mActivity, LoginActivity.class);
                                mActivity.startActivity(intent);
                                mActivity.finish();
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    startActivity(new Intent(NewSignInActivity.this, MainActivity.class));
                    finish();
                    break;
                default:
                    break;
            }
        }
    }

    protected void showTip(final String str) {

        mToast.setText(str);
        mToast.show();

    }
}
