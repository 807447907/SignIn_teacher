package cn.luozy.signin.signin_teacher;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
import static cn.luozy.signin.signin_teacher.Utility.postRequest;

public class SignInActivity extends AppCompatActivity {
    private final String showCourseListURL = "https://signin.luozy.cn/api/teacher/course";
    private final String course_root_url = "https://signin.luozy.cn/api/teacher/course/";

    private Toast mToast;

    private String teacher_id = "user_id";
    private String teacher_token = "token";
    private final String json_error_message = "errmsg";

    private int course_id;
    private int sign_in_id;

    private String mCourseName;
    private String mSignInName;
    private String mTime;
    private int mNumSigned;
    private boolean mSignInEnable;

    private SharedPreferences mSharedPref;

    private TextView textViewCourseName;
    private TextView textViewSignInName;
    private TextView textViewNumSigned;
    private TextView textViewTime;

    private Handler handler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPref = getSharedPreferences(
                getString(R.string.preference_login),
                Context.MODE_PRIVATE
        );
        teacher_id = mSharedPref.getString(getString(R.string.preference_id_key), null);
        teacher_token = mSharedPref.getString(getString(R.string.preference_token_key), null);
        setContentView(R.layout.activity_sign_in);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        sign_in_id = intent.getIntExtra("sign_in_id", 0);
        course_id = intent.getIntExtra("course_id", 0);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new FAButtonListener());
        updateFabState();

        textViewCourseName = (TextView)findViewById(R.id.text_view_course_name);
        textViewSignInName = (TextView)findViewById(R.id.text_view_sign_in_name);
        textViewNumSigned = (TextView)findViewById(R.id.text_view_num_signed);
        textViewTime = (TextView)findViewById(R.id.text_view_time);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(){
            @Override
            public void run() {
                attemptGetCourse();
                attemptGetSignIn();
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(
                R.menu.menu_sign_in,
                menu
        );
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_delete:
                new Thread() {
                    @Override
                    public void run() {
                        attemptDelete();
                    }
                }.start();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class FAButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (mSignInEnable) {
                new Thread(){
                    @Override
                    public void run() {
                        attemptStop();
                    }
                }.start();
            } else {
                new Thread(){
                    @Override
                    public void run() {
                        attemptStart();
                    }
                }.start();
            }
        }
    }

    private void attemptGetCourse () {
        Map<String, String> params = new HashMap<>();
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);
        String url = course_root_url + course_id;
        Log.d("DDD", url);
        String resp = postRequest(url, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 10;
            handler.sendMessage(message);
        }
    }

    private void attemptGetSignIn () {
        Map<String, String> params = new HashMap<>();
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);
        String url =
                course_root_url
                + course_id + "/sign_in/"
                + sign_in_id;

        Log.d("DDD", url);
        String resp = postRequest(url, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 1;
            handler.sendMessage(message);
        }
    }

    private void attemptStart() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Utility.showTip(this, getString(R.string.tip_no_bluetooth));
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
            return;
        }

        if (bluetoothAdapter.getScanMode() != SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(intent, 1);
            return;
        }

        final String address = bluetoothAdapter.getAddress();

        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);

        String url = course_root_url + course_id
                + "/sign_in/" + sign_in_id + "/start";

        String resp = postRequest(url, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.json_response), resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 2;
            handler.sendMessage(message);
        }
    }

    private void attemptStop() {
        Map<String, String> params = new HashMap<>();
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);

        String url = course_root_url + course_id
                + "/sign_in/" + sign_in_id + "/stop";

        String resp = postRequest(url, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.json_response), resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 3;
            handler.sendMessage(message);
        }
    }

    private void attemptDelete() {
        Map<String, String> params = new HashMap<>();
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);

        String url = course_root_url + course_id
                + "/sign_in/" + sign_in_id + "/delete";

        String resp = postRequest(url, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.json_response), resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 4;
            handler.sendMessage(message);
        }
    }

    private void updateFabState() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (mSignInEnable) {
            fab.setImageResource(R.drawable.ic_action_stop);
            fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red)));
        } else {
            fab.setImageResource(R.drawable.ic_action_begin);
            fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green)));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    attemptStart();
                } else if (resultCode == RESULT_CANCELED) {
                    Utility.showTip(this, getString(R.string.tip_please_allow_bluetooth));
                }
                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    attemptStart();
                } else if (resultCode == RESULT_CANCELED) {
                    Utility.showTip(this, getString(R.string.tip_please_allow_bluetooth_discoverable));
                }
            default:
                break;
        }
    }

    class MyHandler extends Handler {
        private SignInActivity mActivity;

        public MyHandler (SignInActivity activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String resp;
            ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
            switch (msg.what) {
                case 10:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(SignInActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            mCourseName = jsonObject.getString("name");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    textViewCourseName.setText(mCourseName);
                    break;
                case 1:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(SignInActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            mSignInName = jsonObject.getString("name");
                            mSignInEnable = jsonObject.getBoolean("state");
                            mTime = jsonObject.getString("updated_at");

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    textViewSignInName.setText(mSignInName);
                    textViewTime.setText(mTime);
                    textViewNumSigned.setText("10");
                    updateFabState();
                    break;
                case 2:
                    resp = msg.getData().getString(getString(R.string.json_response));
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(SignInActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            Utility.showTip(SignInActivity.this, jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSignInEnable = true;
                    updateFabState();
                    break;
                case 3:
                    resp = msg.getData().getString(getString(R.string.json_response));
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(SignInActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            Utility.showTip(SignInActivity.this, jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSignInEnable = false;
                    updateFabState();
                    break;
                case 4:
                    resp = msg.getData().getString(getString(R.string.json_response));
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(SignInActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            Utility.showTip(SignInActivity.this, jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    startActivity(new Intent(SignInActivity.this, MainActivity.class));
                    finish();
                    break;
                default:
                    break;
            }
        }
    }

}
