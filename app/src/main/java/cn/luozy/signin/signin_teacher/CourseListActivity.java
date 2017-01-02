package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static cn.luozy.signin.signin_teacher.Utility.postRequest;

class ViewHolder {
    TextView textViewCourseName;
    ImageButton buttonDelete;
}

class CourseAdapter extends BaseAdapter {
    private CourseListActivity mContext;
    private ArrayList<HashMap<String, Object>> courseList = new ArrayList<>();

    CourseAdapter (CourseListActivity context, ArrayList<HashMap<String, Object>> arrayList) {
        mContext = context;
        courseList = arrayList;
    }

    @Override
    public int getCount() {
        return courseList.size();
    }

    @Override///////////////
    public Object getItem(int position) {
        return null;
    }

    @Override//////////////////
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.course_list_item, null);
            viewHolder = new ViewHolder();

            viewHolder.textViewCourseName = (TextView) convertView.findViewById(R.id.textview_course_name);
            viewHolder.buttonDelete = (ImageButton) convertView.findViewById(R.id.image_button_delete);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Integer course_id = (int) courseList.get(position).get("course_id");
        final String course_name = courseList.get(position).get("course_name").toString();

        viewHolder.textViewCourseName.setText(course_name);

        viewHolder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        mContext.attemptDeleteCourse(course_id);
                        mContext.attemptGetCourseList();
                    }
                }.start();
            }
        });

        return convertView;
    }
}

public class CourseListActivity extends AppCompatActivity {
    private final String json_teacher_id = "user_id";
    private final String json_teacher_token = "token";
    private final String json_error_message = "errmsg";
    private final String json_course_name = "name";
    private final String json_description = "descripstion";

    private final String show_list_url = "https://signin.luozy.cn/api/teacher/course";
    private final String create_course_url = "https://signin.luozy.cn/api/teacher/course/create";
    private final String course_root_url = "https://signin.luozy.cn/api/teacher/course/";

    private String teacher_id;
    private String teacher_token;
    private String new_course_name;
    private String new_course_description;

    private SharedPreferences mSharedPref;

    private ListView listview_course_list;
    protected CourseAdapter mAdapter;
    protected ArrayList<HashMap<String, Object>> courseList = new ArrayList<>();
    private Handler handler = new MyHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPref = getSharedPreferences(
                getString(R.string.preference_login),
                Context.MODE_PRIVATE
        );
        teacher_id = mSharedPref.getString(getString(R.string.preference_id_key), null);
        teacher_token = mSharedPref.getString(getString(R.string.preference_token_key), null);
        setContentView(R.layout.activity_course_list);

        mAdapter = new CourseAdapter(this, courseList);
        listview_course_list = (ListView) findViewById(R.id.list_view_course);
        listview_course_list.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCourseList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.course_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add_course:
                LayoutInflater inflater = getLayoutInflater();
                final View layout = inflater.inflate(
                        R.layout.dialog_new_course,
                        (ViewGroup) findViewById(R.id.dialog_new_course));

                AlertDialog.Builder alertDialogBuilder=new AlertDialog.Builder(this);
                alertDialogBuilder.setView(layout);
                final EditText edit_text_course_name = (EditText) layout.findViewById(R.id.edit_text_course_name);
                final EditText edit_text_description = (EditText) layout.findViewById(R.id.edit_text_description);
                alertDialogBuilder.setPositiveButton("创建",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new_course_name = edit_text_course_name.getText().toString();
                                new_course_description = edit_text_description.getText().toString();
                                if (new_course_name.length() == 0) {
                                    Utility.showTip(CourseListActivity.this, "请指定课程名");
                                } else {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            attemptCreateCourse();
                                            attemptGetCourseList();
                                        }
                                    }.start();
                                }
                            }
                        });
                alertDialogBuilder.setNegativeButton("取消", null);
                alertDialogBuilder.setTitle(Html.fromHtml("<font color='#3366cc'>新建课程</font>"));
                alertDialogBuilder.create().show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void attemptGetCourseList() {
        Log.d("DDD", "attempt get coutse list");
        Map<String, String> params = new HashMap<>();
        params.put(json_teacher_id, teacher_id);
        params.put(json_teacher_token, teacher_token);

        String resp = postRequest(show_list_url, params);
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

    private void attemptCreateCourse() {
        Map<String, String> params = new HashMap<>();
        params.put(json_teacher_id, teacher_id);
        params.put(json_teacher_token, teacher_token);
        params.put(json_course_name, new_course_name);
        params.put(json_description, new_course_description);

        String resp = postRequest(create_course_url, params);
        Log.d("DDD", resp);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 11;
            handler.sendMessage(message);
        }
    }

    protected void attemptDeleteCourse (int course_id) {
        Map<String, String> params = new HashMap<>();
        params.put(json_teacher_id, teacher_id);
        params.put(json_teacher_token, teacher_token);
        String delete_course_url = course_root_url+course_id+"/delete";
        Log.d("DDD", delete_course_url);

        String resp = postRequest(delete_course_url, params);
        Log.d("DDD", resp);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 12;
            handler.sendMessage(message);
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String resp;
            switch (msg.what) {
                case 10:
                    Log.d("DDD", ""+msg.what);
                    resp = msg.getData().getString("resp");
                    ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(CourseListActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(CourseListActivity.this, LoginActivity.class);
                            startActivity(intent);
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
                    mAdapter.notifyDataSetChanged();
                    break;

                case 11:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(CourseListActivity.this, jsonObject.getString(json_error_message));
                            return;
                        } else {
                            Utility.showTip(CourseListActivity.this, jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case 12:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(CourseListActivity.this, jsonObject.getString(json_error_message));
                            return;
                        } else {
                            Utility.showTip(CourseListActivity.this, jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private void refreshCourseList() {
        if (Utility.checkConnection(this)) {
            new Thread() {
                public void run() {
                    attemptGetCourseList();
                }
            }.start();
        }
    }
}
