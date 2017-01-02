package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static cn.luozy.signin.signin_teacher.Utility.postRequest;

public class NewSignInActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final String showCourseListURL = "https://signin.luozy.cn/api/teacher/course";
    private final String course_root_url = "https://signin.luozy.cn/api/teacher/course/";

    private String teacher_id;
    private String teacher_token;

    private Spinner mSpinner;

    private SharedPreferences mSharedPref;

    private final String json_teacher_id = "user_id";
    private final String json_teacher_token = "token";
    private final String json_error_message = "errmsg";

    private int current_course_index;
    private int current_course_id;

    protected ArrayList<HashMap<String, Object>> courseList = new ArrayList<>();

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

        mSpinner = (Spinner)findViewById(R.id.spinnerCourse);
        mSpinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread() {
            @Override
            public void run() {
                attemptGetCourseList();
            }
        }.start();
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
            final String sign_in_name = ((EditText)findViewById(R.id.editTextSignInName))
                    .getText().toString();
            if (sign_in_name.length() > 0) {
                new Thread() {
                    public void run() {
                        attemptCreate(sign_in_name);
                    }
                }.start();
            } else {
                Utility.showTip(this, "签到名称不能为空");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        current_course_index = i;
        current_course_id = (int)courseList.get(i).get("course_id");
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    protected void attemptGetCourseList() {
        Log.d("DDD", "attempt get coutse list");
        Map<String, String> params = new HashMap<>();
        params.put(json_teacher_id, teacher_id);
        params.put(json_teacher_token, teacher_token);

        String resp = postRequest(showCourseListURL, params);
        Log.d("DDD", resp);
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

    protected void attemptCreate(final String sign_in_name) {
        Map<String, String> params = new HashMap<>();
        params.put("name", sign_in_name);
        params.put(getString(R.string.json_teacher_id), teacher_id);
        params.put(getString(R.string.json_teacher_token), teacher_token);

        String url_create = course_root_url
                        + current_course_id
                        + "/sign_in/create";

        Log.d("DDD", url_create);

        String resp = postRequest(url_create, params);
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
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(NewSignInActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(NewSignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(NewSignInActivity.this, MainActivity.class);
                    intent.putExtra("course_index", current_course_index);
                    startActivity(intent);
                    finish();
                    break;
                case 10:
                    Log.d("DDD", ""+msg.what);
                    resp = msg.getData().getString("resp");
                    ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(NewSignInActivity.this, jsonObject.getString(json_error_message));
                            Intent mIntent = new Intent(NewSignInActivity.this, LoginActivity.class);
                            startActivity(mIntent);
                            finish();
                            return;
                        } else {
                            JSONArray data = jsonObject.getJSONArray("list");
                            Log.d("DDD", ""+data.length());
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                HashMap<String, Object> params = new HashMap<>();
                                params.put("course_id", item.getInt("course_id"));
                                params.put("course_name", item.getString("name"));
                                tempList.add(params);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    courseList.clear();
                    courseList.addAll(tempList);

                    ArrayList<String> courseItems = new ArrayList<String>();
                    for (int i = 0; i < courseList.size(); i++)
                        courseItems.add(courseList.get(i).get("course_name").toString());

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            NewSignInActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            courseItems);
                    mSpinner.setAdapter(adapter);
                    break;
                default:
                    break;
            }
        }
    }
}
