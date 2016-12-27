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
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.Map;

import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;

public class SignInActivity extends AppCompatActivity {

    private Toast mToast;

    private String teacher_id;
    private String teacher_token;

    private SharedPreferences mSharedPref;

    private int mSignInId;
    private String mSignInName;
    private boolean mSignInEnable;

    private TextView textViewSignInName;
    private TextView textViewSignInId;

    private Handler handler = new MyHandler(this);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(
                R.menu.menu_sign_in,
                menu
        );
        return super.onCreateOptionsMenu(menu);
    }

    private class FAButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (mSignInEnable) {
                attemptStop(mSignInId);
            } else {
                attemptStart(mSignInId);
            }
        }
    }

    private void attemptStart(final Integer signIn_id) {
        new Thread() {
            @Override
            public void run() {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                if (bluetoothAdapter == null) {
                    showTip(getString(R.string.tip_no_bluetooth));
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

                final String signIn_bluetooth = bluetoothAdapter.getAddress();

                Map<String, String> params = new HashMap<>();
                params.put(getString(R.string.json_sign_in_id), signIn_id.toString());
                params.put(getString(R.string.json_sign_in_bluetooth), signIn_bluetooth);
                params.put(getString(R.string.json_teacher_id), teacher_id);
                params.put(getString(R.string.json_teacher_token), teacher_token);

                String resp = MainActivity.postRequest(getString(R.string.url_start), params);
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
        }.start();
    }

    private void attemptStop(final Integer signIn_id) {
        new Thread() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<>();
                params.put(getString(R.string.json_sign_in_id), signIn_id.toString());
                params.put(getString(R.string.json_teacher_id), teacher_id);
                params.put(getString(R.string.json_teacher_token), teacher_token);

                String resp = MainActivity.postRequest(getString(R.string.url_stop), params);
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
        }.start();
    }

    protected void showTip(final String str) {
        if (str == null) {
            mToast.setText("the toast is lost!");
        } else {
            mToast.setText(str);
        }
        mToast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mSignInId = intent.getIntExtra(getString(R.string.json_sign_in_id), -1);
        mSignInName = intent.getStringExtra("SignIn_name");
        mSignInEnable = intent.getBooleanExtra("SignIn_enable", false);

        mSharedPref = getSharedPreferences(
                getString(R.string.preference_login),
                Context.MODE_PRIVATE
        );
        teacher_id = mSharedPref.getString(getString(R.string.preference_id_key), null);
        teacher_token = mSharedPref.getString(getString(R.string.preference_token_key), null);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new FAButtonListener());
        updateFabState();

        textViewSignInId = (TextView) findViewById(R.id.textview_sign_in_id);
        textViewSignInName = (TextView) findViewById(R.id.textview_sign_in_name);

        textViewSignInId.setText("第 " + mSignInId + " 次");
        textViewSignInName.setText(mSignInName);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
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
                    attemptStart(mSignInId);
                } else if (resultCode == RESULT_CANCELED) {
                    showTip(getString(R.string.tip_please_allow_bluetooth));
                }
                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    attemptStart(mSignInId);
                } else if (resultCode == RESULT_CANCELED) {
                    showTip(getString(R.string.tip_please_allow_bluetooth_discoverable));
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
            switch (msg.what) {
                case 2:
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
                            } else if (errors.has(getString(R.string.json_sign_in_id))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_sign_in_id)).getString(0);
                            } else if (errors.has(getString(R.string.json_sign_in_bluetooth))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_sign_in_bluetooth)).getString(0);
                            } else {
                                errorMsg = "蜜汁错误";
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
                    mSignInEnable = true;
                    updateFabState();
                    break;
                case 3:
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
                            } else if (errors.has(getString(R.string.json_sign_in_id))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_sign_in_id)).getString(0);
                            } else if (errors.has(getString(R.string.json_sign_in_bluetooth))) {
                                errorMsg = errors.getJSONArray(getString(R.string.json_sign_in_bluetooth)).getString(0);
                            } else {
                                errorMsg = "蜜汁错误";
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
                    mSignInEnable = false;
                    updateFabState();
                    break;
                default:
                    break;
            }
        }
    }

}
