package cn.youyunpushdome;

import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.weimi.push.WeimiPush;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import matrix.sdk.WeimiInstance;
import matrix.sdk.data.AuthResultData;
import matrix.sdk.message.HistoryMessage;
import matrix.sdk.message.WChatException;
import matrix.sdk.util.HttpCallback;

public class LoginActivity extends AppCompatActivity {

    private String TAG = "Bill";
    private EditText editStart;
    private EditText editEnd;
    private TextView textGetInfo;
    private TextView textSetTime;
    private boolean login = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editStart = (EditText) findViewById(R.id.edit_start);
        editEnd = (EditText) findViewById(R.id.edit_end);
        textGetInfo = (TextView) findViewById(R.id.text_get);
        textSetTime = (TextView) findViewById(R.id.text_set);
    }

    public void handleLogin(View v) {
        login();
    }

    public void handleStartPush(View v) {
        startPush();
    }

    public void handleGetPushInfo(View v) {
        getInfo();
    }

    public void handleSetPushInfo(View v) {
        set();
    }

    private void login() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String udid = generateOpenUDID();
                    String clientIdDefault = "1-20082-3e1632fba5606ffd129dd9d08b8df64a-android";
                    String clientSecretDefault = "c5bcda37516e2c8dbd41baa2df74821c";
                    AuthResultData authResultData = WeimiInstance.getInstance().registerApp(getApplicationContext(), udid,
                                    clientIdDefault, clientSecretDefault, 30);

                    if (authResultData.success) {
                        login = true;
                        final String uid = WeimiInstance.getInstance().getUID();
                        Log.v(TAG, "uid:" + uid);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } catch (WChatException e) {
                    e.printStackTrace();
                }

            }

        }).start();

    }

    private void startPush(){
        if(!login)
            return;
        boolean startpush = WeimiPush.connect(
                LoginActivity.this.getApplicationContext(),
                WeimiPush.pushServerIp, false);
        if (startpush) {
            Toast.makeText(LoginActivity.this, "PUSH服务启动成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(LoginActivity.this, "PUSH服务启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void set() {
        String start = editStart.getText().toString().trim();
        String end = editEnd.getText().toString().trim();
        if (TextUtils.isEmpty(start) || TextUtils.isEmpty(end)) {
            return;
        }
        WeimiInstance.getInstance().shortPushCreate(start, end,
                new HttpCallback() {

                    @Override
                    public void onResponseHistory(
                            List<HistoryMessage> historyMessage) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onResponse(String result) {
                        if (result != null) {
                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(result);
                                String code = jsonObject
                                        .optString("code", null);
                                if (code != null && code.equals("200")) {
                                    Log.v(TAG, "result:" + result);
                                    Message message = handler.obtainMessage();
                                    message.what = 2;
                                    message.obj = "set success";
                                    handler.sendMessage(message);
                                }
                            } catch (JSONException e) {
                                Message message = handler.obtainMessage();
                                message.what = 2;
                                message.obj = "error";
                                handler.sendMessage(message);
                                e.printStackTrace();
                            }
                        }

                    }

                    @Override
                    public void onError(Exception e) {
                        Log.v(TAG, "e:" + e.getMessage());
                        Message message = handler.obtainMessage();
                        message.what = 2;
                        message.obj = "error";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onResponse(byte[] arg0) {
                        // TODO Auto-generated method stub

                    }
                }, 60);
    }

    private void getInfo() {
        WeimiInstance.getInstance().shortPushShowUser(new HttpCallback() {

            @Override
            public void onResponseHistory(List<HistoryMessage> arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onResponse(String result) {
                if (result != null) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(result);
                        String code = jsonObject.optString("code", null);
                        if (code != null && code.equals("200")) {
                            String msg = jsonObject.optJSONObject("msg").toString();
                            Log.v(TAG, "msg:" + msg);
                            if (!TextUtils.isEmpty(msg)){
                                Message message = handler.obtainMessage();
                                message.what = 1;
                                message.obj = msg;
                                handler.sendMessage(message);
                            }
                        }
                    } catch (JSONException e) {
                        Message message = handler.obtainMessage();
                        message.what = 1;
                        message.obj = "error";
                        handler.sendMessage(message);
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onError(Exception e) {
                Message message = handler.obtainMessage();
                message.what = 1;
                message.obj = "error";
                handler.sendMessage(message);
                Log.v(TAG, "e:" + e.getMessage());

            }

            @Override
            public void onResponse(byte[] arg0) {
                // TODO Auto-generated method stub

            }
        }, 120);
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    String str = (String) msg.obj;
                    textGetInfo.setText(str);
                    break;
                case 2:
                    String message = (String) msg.obj;
                    textSetTime.setText(message);
                    break;
                default:
                    break;
            }
        };
    };

    /**
     * 根据设备生成一个唯一标识
     *
     * @return
     */
    private String generateOpenUDID() {
        // Try to get the ANDROID_ID
        String OpenUDID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (OpenUDID == null || OpenUDID.equals("9774d56d682e549c")
                | OpenUDID.length() < 15) {
            // if ANDROID_ID is null, or it's equals to the GalaxyTab generic
            // ANDROID_ID or bad, generates a new one
            final SecureRandom random = new SecureRandom();
            OpenUDID = new BigInteger(64, random).toString(16);
        }
        return OpenUDID;
    }
}
