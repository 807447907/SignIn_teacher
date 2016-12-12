package cn.luozy.signin.signin_teacher;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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

import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;

public class MainActivity extends AppCompatActivity {
    private final String createURL = "https://signin.luozy.cn/api/teacher/create";
    private final String startURL = "https://signin.luozy.cn/api/teacher/start";
    private final String stopURL = "https://signin.luozy.cn/api/teacher/stop";
    private final String showListURL = "https://signin.luozy.cn/api/teacher/list";

    private String teacher_id;
    private String teacher_token;
    private Toast mToast;

    private MyAdapter mAdapter;
    private ArrayList<HashMap<String, Object>> signInList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

        teacher_id = getIntent().getStringExtra("teacher_id");
        teacher_token = getIntent().getStringExtra("teacher_token");

        Button button = (Button) findViewById(R.id.buttonCreate);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextSignInName = (EditText) findViewById(R.id.editTextSignInName);
                final String SignInName = editTextSignInName.getText().toString();
                new Thread() {
                    public void run() {
                        attemptCreate(SignInName);
                    }
                }.start();
            }
        });

        mAdapter = new MyAdapter(this);
        ListView listViewSignIn = (ListView) findViewById(R.id.listViewSignIn);
        listViewSignIn.setAdapter(mAdapter);
        refreshSignInList();
    }

    private void refreshSignInList() {
        new Thread() {
            public void run() {
                attemptGetList();
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                if (resultCode == 0) {
                    showTip("请允许开启蓝牙设备");
                } else {
                    showTip("请确保您的蓝牙设备可被发现");
                }
                break;
            default:
                break;
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String resp;
            switch (msg.what) {
                case 0:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONTokener jsonTokener = new JSONTokener(resp);
                        JSONObject jsonObject = (JSONObject) jsonTokener.nextValue();
                        if (jsonObject.getInt("status") == 0) {
                            showTip(jsonObject.getString("msg"));
                        } else {
                            JSONObject errors = jsonObject.getJSONObject("errors");
                            boolean exit = false;
                            String errorMsg = null;
                            if (errors.has("teacher_id")) {
                                errorMsg = errors.getJSONArray("teacher_id").getString(0);
                                exit = true;
                            } else if (errors.has("teacher_token")) {
                                errorMsg = errors.getJSONArray("teacher_token").getString(0);
                                exit = true;
                            } else if (errors.has("signIn_name")) {
                                errorMsg = errors.getJSONArray("signIn_name").getString(0);
                            }
                            showTip(errorMsg);
                            if (exit) {
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    refreshSignInList();
                    break;
                case 1:
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
                    break;
                case 2:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONTokener jsonTokener = new JSONTokener(resp);
                        JSONObject jsonObject = (JSONObject) jsonTokener.nextValue();
                        if (jsonObject.getInt("status") == 0) {
                            showTip(jsonObject.getString("msg"));
                        } else {
                            JSONObject errors = jsonObject.getJSONObject("errors");
                            boolean exit = false;
                            String errorMsg = null;
                            if (errors.has("teacher_id")) {
                                errorMsg = errors.getJSONArray("teacher_id").getString(0);
                                exit = true;
                            } else if (errors.has("teacher_token")) {
                                errorMsg = errors.getJSONArray("teacher_token").getString(0);
                                exit = true;
                            } else if (errors.has("signIn_id")) {
                                errorMsg = errors.getJSONArray("signIn_id").getString(0);
                            } else if (errors.has("signIn_bluetooth")) {
                                errorMsg = errors.getJSONArray("signIn_bluetooth").getString(0);
                            }
                            showTip(errorMsg);
                            if (exit) {
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    refreshSignInList();
                    break;
                case 3:
                    resp = msg.getData().getString("resp");
                    try {
                        JSONTokener jsonTokener = new JSONTokener(resp);
                        JSONObject jsonObject = (JSONObject) jsonTokener.nextValue();
                        if (jsonObject.getInt("status") == 0) {
                            showTip(jsonObject.getString("msg"));
                        } else {
                            JSONObject errors = jsonObject.getJSONObject("errors");
                            boolean exit = false;
                            String errorMsg = null;
                            if (errors.has("teacher_id")) {
                                errorMsg = errors.getJSONArray("teacher_id").getString(0);
                                exit = true;
                            } else if (errors.has("teacher_token")) {
                                errorMsg = errors.getJSONArray("teacher_token").getString(0);
                                exit = true;
                            } else if (errors.has("signIn_id")) {
                                errorMsg = errors.getJSONArray("signIn_id").getString(0);
                            } else if (errors.has("signIn_bluetooth")) {
                                errorMsg = errors.getJSONArray("signIn_bluetooth").getString(0);
                            }
                            showTip(errorMsg);
                            if (exit) {
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    refreshSignInList();
                    break;
                default:
                    break;
            }
        }
    };

    private void attemptCreate(final String signIn_name) {
        Map<String, String> params = new HashMap<>();
        params.put("signIn_name", signIn_name);
        params.put("teacher_id", teacher_id);
        params.put("teacher_token", teacher_token);

        String resp = postRequest(createURL, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 0;
            handler.sendMessage(message);
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

    private void attemptStart(final Integer signIn_id) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showTip("您的设备不支持蓝牙");
            return;
        }

        if (!bluetoothAdapter.isEnabled() || bluetoothAdapter.getScanMode() != SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(intent, 0);
            return;
        }

        final String signIn_bluetooth = bluetoothAdapter.getAddress();
        Map<String, String> params = new HashMap<>();
        params.put("signIn_id", signIn_id.toString());
        params.put("signIn_bluetooth", signIn_bluetooth);
        params.put("teacher_id", teacher_id);
        params.put("teacher_token", teacher_token);

        String resp = postRequest(startURL, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 2;
            handler.sendMessage(message);
        }

    }

    private void attemptStop(final Integer signIn_id) {
        Map<String, String> params = new HashMap<>();
        params.put("signIn_id", signIn_id.toString());
        params.put("teacher_id", teacher_id);
        params.put("teacher_token", teacher_token);

        String resp = postRequest(stopURL, params);
        //noinspection StringEquality
        if (!resp.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("resp", resp);
            Message message = new Message();
            message.setData(bundle);
            message.what = 3;
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

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return signInList.size();
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

            View myView = mInflater.inflate(R.layout.list_item, null);

            TextView textViewIndex = (TextView) myView.findViewById(R.id.textViewIndex);
            TextView textViewTitle = (TextView) myView.findViewById(R.id.textViewTitle);
            Button buttonStart = (Button) myView.findViewById(R.id.buttonStart);
            Button buttonStop = (Button) myView.findViewById(R.id.buttonStop);

            final Integer signIn_id = (int) signInList.get(position).get("signIn_id");
            final String signIn_name = signInList.get(position).get("signIn_name").toString();

            textViewIndex.setText(signIn_id.toString());
            textViewTitle.setText(signIn_name);

            boolean enable = (boolean) signInList.get(position).get("signIn_enable");
            if (enable) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
                buttonStop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Thread() {
                            public void run() {
                                attemptStop(signIn_id);
                            }
                        }.start();
                    }
                });
            } else {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                buttonStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Thread() {
                            public void run() {
                                attemptStart(signIn_id);
                            }
                        }.start();
                    }
                });
            }

            return myView;
        }
    }
}
