package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static cn.luozy.signin.signin_teacher.Utility.postRequest;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SwipeRefreshLayout.OnRefreshListener,
        AdapterView.OnItemClickListener
{

    private final String showCourseListURL = "https://signin.luozy.cn/api/teacher/course";
    private final String course_root_url = "https://signin.luozy.cn/api/teacher/course/";

    private final int course_id_ground_base = 1234;

    private String teacher_id;
    private String teacher_token;
    private String teacher_name;

    private final String json_teacher_id = "user_id";
    private final String json_teacher_token = "token";
    private final String json_error_message = "errmsg";
    private final String json_course_name = "name";

    private NavigationView mNavView;
    private Toolbar mToolbar;

    protected SignInAdapter mAdapter;
    protected ArrayList<HashMap<String, Object>> signInList = new ArrayList<>();
    protected ArrayList<HashMap<String, Object>> courseList = new ArrayList<>();

    private int current_course_index = 0;
    private Handler handler = new MyHandler();

    private SharedPreferences mSharedPref;

    private SwipeRefreshLayout mSwipeRefresh;

    private boolean initialLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPref = getSharedPreferences(
                getString(R.string.preference_login),
                Context.MODE_PRIVATE
        );

        if (!mSharedPref.contains(getString(R.string.preference_token_key))) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        } else {
            teacher_id = mSharedPref.getString(getString(R.string.preference_id_key), null);
            teacher_token = mSharedPref.getString(getString(R.string.preference_token_key), null);
            teacher_name = mSharedPref.getString(getString(R.string.preference_name_key), null);
        }

        current_course_index = getIntent().getIntExtra("course_index", 0);

        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mNavView = (NavigationView) findViewById(R.id.nav_view);
        View navHeader = mNavView.getHeaderView(0);
        ((TextView)navHeader.findViewById(R.id.text_view_nav_header_name)).setText(teacher_name);
        ((TextView)navHeader.findViewById(R.id.text_view_nav_header_id)).setText(teacher_id);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NewSignInActivity.class);
                startActivity(intent);
            }
        });

        mSwipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_2,
                R.color.refresh_progress_3);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mAdapter = new SignInAdapter(this, signInList);
        ListView listViewSignIn = (ListView) findViewById(R.id.listViewSignIn);
        listViewSignIn.setAdapter(mAdapter);
        listViewSignIn.setOnItemClickListener(this);

        initialLoaded = false;
        refreshNavCourseList();
    }

    @Override
    protected void onResume() {
        if (teacher_token != null && initialLoaded) {
            refreshNavCourseList();
            refreshSignInList();
        }
        super.onResume();
    }

    @Override
    public void onRefresh() {
        refreshSignInList();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    protected void refreshSignInList() {
        if (current_course_index < courseList.size()) {
            if (Utility.checkConnection(this)) {
                new Thread() {
                    public void run() {
                        attemptGetList();
                    }
                }.start();
            }
        }
    }

    void refreshNavCourseList() {
        mNavView = (NavigationView) findViewById(R.id.nav_view);
        Menu navMenu = mNavView.getMenu();
        for (int i = 0; i < courseList.size(); i++) {
            navMenu.removeItem(course_id_ground_base+i);
        }
        new Thread(){
            @Override
            public void run() {
                attemptGetCourseList();
            }
        }.start();
    }

    private void attemptGetList() {
            Map<String, String> params = new HashMap<>();
            params.put(json_teacher_id, teacher_id);
            params.put(json_teacher_token, teacher_token);
            String showSignInListURL =
                    course_root_url
                            + (int)courseList.get(current_course_index).get("course_id")
                            +"/sign_in";
            Log.d("DDD", showSignInListURL);
            String resp = postRequest(showSignInListURL, params);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra("course_id", (int)courseList.get(current_course_index).get("course_id"));
        intent.putExtra("sign_in_id", (int)signInList.get(i).get("sign_in_id"));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.nav_about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("15211121 罗振宇\n\n15211023 杨卓谦");
                builder.setTitle("关于");
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {@Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                       }
                });
                builder.create().show();
                break;
            default:
                break;
        }
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_log_out:
                mSharedPref.edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                break;
            case R.id.nav_edit_course:
                startActivity(new Intent(this, CourseListActivity.class));
                break;
            default:
                current_course_index = id-course_id_ground_base;
                refreshSignInList();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

    class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String resp;
            ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
            switch (msg.what) {
                case 1:
                    mSwipeRefresh.setRefreshing(true);
                    resp = msg.getData().getString("resp");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(MainActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            String course_name = courseList.get(current_course_index).get("course_name").toString();
                            mToolbar.setTitle(course_name);
                            JSONArray data = jsonObject.getJSONArray("list");
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                HashMap<String, Object> params = new HashMap<>();
                                params.put("sign_in_id", item.getInt("sign_in_id"));
                                params.put("sign_in_course", course_name);
                                params.put("sign_in_name", item.getString("name"));
                                params.put("sign_in_state", item.getBoolean("state"));
                                params.put("sign_in_time", item.getString("updated_at"));
                                params.put("sign_in_num_records", item.getInt("records"));
                                tempList.add(params);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    signInList.clear();
                    signInList.addAll(tempList);
                    mAdapter.notifyDataSetChanged();
                    mSwipeRefresh.setRefreshing(false);
                    break;
                case 10:
                    Log.d("DDD", ""+msg.what);
                    resp = msg.getData().getString("resp");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        Log.d("DDD", "in Handler: "+resp);
                        if (jsonObject.has(json_error_message)) {
                            Utility.showTip(MainActivity.this, jsonObject.getString(json_error_message));
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
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

                    mNavView = (NavigationView) findViewById(R.id.nav_view);
                    Menu navMenu = mNavView.getMenu();
                    for (int i = 0; i < courseList.size(); i++) {
                        navMenu.add(0,
                                course_id_ground_base+i,
                                Menu.NONE,
                                courseList.get(i).get("course_name").toString())
                                .setIcon(getDrawable(R.drawable.ic_bullet_point));
                    }

                    if (!initialLoaded) {
                        if (current_course_index < courseList.size())
                            refreshSignInList();
                        initialLoaded = true;
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
