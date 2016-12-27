package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SwipeRefreshLayout.OnRefreshListener  {

    private String showListURL;

    private String teacher_id;
    private String teacher_token;
    private Toast mToast;

    protected SignInAdapter mAdapter;
    protected ArrayList<HashMap<String, Object>> signInList = new ArrayList<>();

    private Handler handler = new MyHandler();

    private SharedPreferences mSharedPref;

    private SwipeRefreshLayout mSwipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showListURL = getString(R.string.url_show_list);
        mSharedPref = getSharedPreferences(
                getString(R.string.preference_login),
                Context.MODE_PRIVATE
        );

        if (!mSharedPref.contains(getString(R.string.preference_token_key))) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        teacher_id = mSharedPref.getString(getString(R.string.preference_id_key), null);
        teacher_token = mSharedPref.getString(getString(R.string.preference_token_key), null);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

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
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mAdapter = new SignInAdapter(this, signInList);
        ListView listViewSignIn = (ListView) findViewById(R.id.listViewSignIn);
        listViewSignIn.setAdapter(mAdapter);

        listViewSignIn.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                intent.putExtra("SignIn_id", (int)signInList.get(i).get(getString(R.string.json_sign_in_id)));
                intent.putExtra("SignIn_name", (String)signInList.get(i).get(getString(R.string.json_sign_in_name)));
                intent.putExtra("SignIn_enable", (boolean)signInList.get(i).get(getString(R.string.json_sign_in_enable)));
                startActivity(intent);
            }
        });
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
        ConnectivityManager conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo == null) {
            showTip(getString(R.string.tip_no_connection));
        } else {
            new Thread() {
                public void run() {
                    attemptGetList();
                }
            }.start();
        }
    }

    private void attemptGetList() {
        Map<String, String> params = new HashMap<>();
        params.put("teacher_id", teacher_id);
        params.put("teacher_token", teacher_token);

        String resp = postRequest(showListURL, params);
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

    protected void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
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

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        refreshSignInList();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_log_out) {
            mSharedPref.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        refreshSignInList();
        super.onResume();
    }

    class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String resp;
            switch (msg.what) {
                case 1:
                    mSwipeRefresh.setRefreshing(true);
                    resp = msg.getData().getString("resp");
                    ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
                    try {
                        JSONTokener jsonTokener = new JSONTokener(resp);
                        JSONObject jsonObject = (JSONObject) jsonTokener.nextValue();
                        if (jsonObject.getInt("status") != 0) {
                            JSONObject errors = jsonObject.getJSONObject("errors");
                            boolean exit = false;
                            String errorMsg = null;
                            if (errors.has("teacher_id")) {
                                errorMsg = errors.getJSONArray("teacher_id").getString(0);
                                exit = true;
                            } else if (errors.has("teacher_token")) {
                                errorMsg = errors.getJSONArray("teacher_token").getString(0);
                                exit = true;
                            }
                            showTip(errorMsg);
                            if (exit) {
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                        JSONArray data = jsonObject.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            HashMap<String, Object> params = new HashMap<>();
                            params.put("signIn_id", item.getInt("signIn_id"));
                            params.put("signIn_name", item.getString("signIn_name"));
                            params.put("signIn_enable", item.getBoolean("signIn_enable"));
                            tempList.add(params);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    signInList.clear();
                    signInList.addAll(tempList);
                    mAdapter.notifyDataSetChanged();
                    mSwipeRefresh.setRefreshing(false);
                    break;
                default:
                    break;
            }
        }
    }


}
