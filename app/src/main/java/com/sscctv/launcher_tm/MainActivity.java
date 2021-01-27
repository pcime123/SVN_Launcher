package com.sscctv.launcher_tm;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.NetworkInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.sscctv.seeeyes.VideoSource;
import com.sscctv.seeeyes.ptz.McuControl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;
import me.toptas.fancyshowcase.OnViewInflateListener;


public class MainActivity extends AppCompatActivity {

    private Timer mTimer;
    private static final String TAG = "Launcher";
    private static final String NETWORK_PREFERENCE = "NETWORK_PREFERENCE";
    private static final String EXTRA_SOURCE = "com.sscctv.seeeyesmonitor.source";
    private static final String SOURCE_SDI = "sdi";
    private static final String SOURCE_AUTO = "auto";
    private static final String SOURCE_HDMI = "hdmi";
    private long mLastClickTime;
    private DataOutputStream opt;
    private Intent mIntent;
    private Intent installIntent;

    private boolean catchValue = false;
    private BroadcastReceiver closeReceiver = null;
    private BroadcastReceiver viewerReceiver = null;

    private Boolean runState = false;
    private NotificationManager nm;


    final String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver receiver;

    @BindView(R.id.main_network)
    Button main_network;
    @BindView(R.id.main_sdi)
    Button main_sdi;
    @BindView(R.id.main_analog)
    Button main_analog;
    @BindView(R.id.main_hdmi)
    Button main_hdmi;
    @BindView(R.id.main_tdrc)
    Button main_tdrc;
    @BindView(R.id.main_tdru)
    Button main_tdru;
    @BindView(R.id.main_apps)
    Button main_apps;
    @BindView(R.id.main_update)
    Button main_update;
    @BindView(R.id.main_packet)
    Button main_packet;

    @BindView(R.id.sub_gallery)
    Button sub_gallery;
    @BindView(R.id.sub_manager)
    Button sub_manager;
    @BindView(R.id.sub_sdcard)
    Button sub_sdcard;
    @BindView(R.id.sub_ethernet)
    Button sub_ethernet;
    @BindView(R.id.sub_browser)
    Button sub_browser;
    @BindView(R.id.sub_setting)
    Button sub_setting;

    @BindView(R.id.time_text)
    TextView timeNow;
    @BindView(R.id.date_text)
    TextView dateNow;

//    @BindView(R.id.ethernet_switch)
//    Switch ethSwitch;


//    @BindView(R.id.sdcard_state)
//    Button sdcard_state;

    private FancyShowCaseView mNetwork, mSdi, mAnalog, mHdmi, mTdr, mEtc, mSub;
    private FancyShowCaseQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        doFirst();

        setContentView(R.layout.activity_main);
//        Log.i(TAG, "Launcher App Data: 201903260921   Version: 2.0.0");


        ButterKnife.bind(this);
        Context mContext = this;
//        startService();

//        MainTimerTask timerTask = new MainTimerTask();
//        mTimer = new Timer();
//        mTimer.schedule(timerTask, 500, 1000);


//        ImageView wifi_state = findViewById(R.id.wifi_state);
//        wifi_state.setImageResource(R.drawable.wifi_out);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);

        portPermission();
        gpioPortSet();

        McuControl mMcuControl = new McuControl();

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }


    private void gpioPortSet() {
        try {
            Runtime command = Runtime.getRuntime();
            Process proc;

            proc = command.exec("su");
            opt = new DataOutputStream(proc.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException();
        }
    }

    private void runGuide() {
        runState = true;

        mNetwork = new FancyShowCaseView.Builder(this)
                .focusOn(main_network)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(290, 280, 500, 330)
                .roundRectRadius(45)
                .customView(R.layout.factory_network, view -> setAnimatedContent(view, mNetwork))
                .build();

        mSdi = new FancyShowCaseView.Builder(this)
                .focusOn(main_sdi)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(790, 280, 395, 330)
                .roundRectRadius(45)
                .customView(R.layout.factory_sdi, view -> setAnimatedContent(view, mSdi))
                .build();

        mAnalog = new FancyShowCaseView.Builder(this)
                .focusOn(main_analog)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(1245, 280, 395, 330)
                .roundRectRadius(45)
                .customView(R.layout.factory_analog, view -> setAnimatedContent(view, mAnalog))
                .build();

        mHdmi = new FancyShowCaseView.Builder(this)
                .focusOn(main_hdmi)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(1690, 280, 395, 330)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(45)
                .customView(R.layout.factory_hdmi, view -> setAnimatedContent(view, mHdmi))
                .build();

        mTdr = new FancyShowCaseView.Builder(this)
                .focusOn(main_tdrc)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(400, 640, 720, 275)
                .roundRectRadius(45)
                .customView(R.layout.factory_tdr, view -> setAnimatedContent(view, mTdr))
                .build();

        mEtc = new FancyShowCaseView.Builder(this)
                .focusOn(main_apps)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(1350, 640, 1080, 275)
                .roundRectRadius(45)
                .customView(R.layout.factory_apps, view -> setAnimatedContent(view, mEtc))
                .build();

        mSub = new FancyShowCaseView.Builder(this)
                .focusOn(sub_gallery)
                .closeOnTouch(false)

//                .disableFocusAnimation()
                .focusRectAtPosition(645, 920, 1150, 200)
                .roundRectRadius(90)
                .customView(R.layout.factory_quick, view -> setAnimatedContent(view, mSub))
                .build();

        mQueue = new FancyShowCaseQueue();
        mQueue.add(mNetwork);
        mQueue.add(mSdi);
        mQueue.add(mAnalog);
        mQueue.add(mHdmi);
        mQueue.add(mTdr);
        mQueue.add(mEtc);
        mQueue.add(mSub);
        mQueue.show();
    }

    private void setAnimatedContent(final View view, final FancyShowCaseView fancyShowCaseView) {
        new Handler().postDelayed(() -> {

            TextView tvNext = view.findViewById(R.id.btn_next);
            TextView tvDismiss = view.findViewById(R.id.btn_dismiss);

            tvNext.setOnClickListener(v -> fancyShowCaseView.hide());

            tvDismiss.setOnClickListener(v -> {
                runState = false;
                mQueue.cancel(true);
            });
        }, 200);
    }

    @OnClick(R.id.main_network)
    void onClick_network() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        mIntent = getPackageManager().getLaunchIntentForPackage("net.biyee.onviferenterprise");
        if (mIntent != null) {
            startService();
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.main_sdi)
    void onClick_sdi() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        installIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.seeeyesmonitor");
        if (installIntent != null) {
            mIntent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
            mIntent.putExtra(EXTRA_SOURCE, SOURCE_SDI);
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.main_analog)
    void onClick_analog() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        installIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.seeeyesmonitor");
        if (installIntent != null) {
            mIntent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
            mIntent.putExtra(EXTRA_SOURCE, SOURCE_AUTO);
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.main_hdmi)
    void onClick_hdmi() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        installIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.seeeyesmonitor");
        if (installIntent != null) {
            mIntent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
            mIntent.putExtra(EXTRA_SOURCE, SOURCE_HDMI);
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.main_tdrc)
    void onClick_tdrc() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        mIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.tdr");
        if (mIntent != null) {
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.main_tdru)
    void onClick_tdru() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        mIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.tdru");
        if (mIntent != null) {
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.main_apps)
    void onClick_apps() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        mIntent = new Intent(this, AppsActivity.class);
        startActivity(mIntent);
    }

    @SuppressLint("WrongConstant")
    @OnClick(R.id.main_update)
    void onClick_update() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        mIntent = getPackageManager().getLaunchIntentForPackage("app_update.sscctv.com.app_update");
        if (mIntent != null) {
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }

//        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//        dialog.setMessage("Preparing...");
//        dialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        AlertDialog alert = dialog.create();
//        alert.show();
    }

    @OnClick(R.id.main_packet)
    void onClick_packet() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 600) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

//        mIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.packetgenerator");
        mIntent = getPackageManager().getLaunchIntentForPackage("com.sscctv.packetAnalyzer");

        if (mIntent != null) {
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
    }

//    @OnClick(R.id.main_settings)
//    void onClick_settings() {
//        startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
//    }

    @OnClick(R.id.sub_gallery)
    void onClick_gallery() {
        mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"));
        startActivity(mIntent);
//        Log.d(TAG, "Long: " + System.currentTimeMillis());
    }

    @OnClick(R.id.sub_manager)
    void onClick_manager() {
        mIntent = getPackageManager().getLaunchIntentForPackage("com.softwinner.explore");
        if (mIntent != null) {
            startActivity(mIntent);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_install), Toast.LENGTH_SHORT).show();
        }
        startActivity(mIntent);
    }

    @OnClick(R.id.sub_ethernet)
    void onClick_ethernet() {
        mIntent = new Intent(this, SettingsEthernet.class);
        startActivity(mIntent);

    }

//    @OnClick(R.id.sub_manual)
//    void onClick_manual() {
//        startActivity(new Intent(this, poeActivity.class));
//test();
//        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//        dialog.setMessage("Preparing...");
//        dialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        AlertDialog alert = dialog.create();
//        alert.show();
//    }

    @OnClick(R.id.sub_setting)
    void onClick_setting() {
        startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
    }

    @OnClick(R.id.sub_sdcard)
    void onClick_sdcard() {
        sdCard_unMount();
//        runGuide();
    }

    @OnClick(R.id.sub_browser)
    void onClick_browser() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText ip = new EditText(this);

        ip.setSingleLine();
        builder.setTitle(getResources().getString(R.string.browser));
        builder.setMessage(getResources().getString(R.string.browser_contents));

        FrameLayout frameLayout = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.apps_horizontal_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.apps_horizontal_margin);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.brower_top);
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.brower_btm);
        ip.setLayoutParams(params);
        frameLayout.addView(ip);


        builder.setView(frameLayout);
        hideSoftKeyboard(ip);

        builder.setPositiveButton(getResources().getString(R.string.close),
                (dialog, which) -> Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_SHORT).show());

        builder.setNeutralButton("Web", (dialog, which) -> {
            String address = "http://www.google.com";
            mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
            startActivity(mIntent);
        });

        builder.setNegativeButton(getResources().getString(R.string.yes),
                (dialog, which) -> {
                    String address = ip.getText().toString();
                    mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + address));
                    startActivity(mIntent);
                });
        builder.show();

    }

    protected void hideSoftKeyboard(EditText mSearchView) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
    }

    private void portPermission() {

        try {
            Runtime command = Runtime.getRuntime();
            Process proc;

            proc = command.exec("su");
            opt = new DataOutputStream(proc.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException();
        }

    }


    private Handler mHandler = new Handler();

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy. MM. dd", Locale.getDefault());
            String formatTime = timeFormat.format(date);
            String formatDate = dateFormat.format(date);
            timeNow.setText(formatTime);
            dateNow.setText(formatDate);
//            isNetworkConnected();
            isEthernet();
        }
    };


    class MainTimerTask extends TimerTask {
        public void run() {
            mHandler.post(mUpdateTimeTask);
        }
    }

    @Override
    protected void onDestroy() {
//        Log.w(TAG, "onDestroy");

//        mTimer.cancel();
        stopStatusService();
        stopBroadCastClose();
        stopBroadCastReceive();
        stopBroadCastSdcard();
        stopBroadCastScreen();
        stopWatchingViewerClose();
//        stopService();
        super.onDestroy();
    }


    public void stopBroadCastScreen() {
        if (mScreen != null) {
            unregisterReceiver(mScreen);
        }
    }

    public void stopBroadCastSdcard() {
        if (mBRSdcard != null) {
            unregisterReceiver(mBRSdcard);
        }
    }


    public void stopBroadCastClose() {
        if (closeReceiver != null) {
            unregisterReceiver(closeReceiver);
        }
    }

    public void stopBroadCastReceive() {
        if (receiver != null) {
            unregisterReceiver(receiver);

        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        mTimer.cancel();

        super.onPause();


    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        buildTimeStamp();
        super.onResume();
        MainTimerTask timerTask = new MainTimerTask();
        mTimer = new Timer();
        mTimer.schedule(timerTask, 500, 3000);


        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mBRSdcard, filter);

        IntentFilter wifi_filter = new IntentFilter(CONNECTIVITY_CHANGE);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        receiver = new IsNetworkReceiver(this);
        registerReceiver(receiver, wifi_filter);

        IntentFilter screen_filter = new IntentFilter();
        screen_filter.addAction(Intent.ACTION_SCREEN_OFF);
        screen_filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreen, screen_filter);

        checkPoeState();

        startWatchingViewerClose();
        startStatusService();
        try {
            sdCard_check();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        if (getPseState().equals("0")) {
//            ethSwitch.setChecked(false);
//            pseDisable();
//        } else if (getPseState().equals("1")) {
//            ethSwitch.setChecked(true);
//            pseEnable();
//        }

    }

    BroadcastReceiver mScreen = new BroadcastReceiver() {
        @Override

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            String packageName;
            ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

            assert am != null;
            packageName = am.getRunningAppProcesses().get(0).processName;
            if (action != null) {
                if (action.equals(Intent.ACTION_SCREEN_ON)) {

                    Log.i(TAG, "Screen ON: " + packageName);
                    if (packageName.equals("net.biyee.onviferenterprise")) {
                        Log.d(TAG, "Screen ON Start Service");
                        startService();
                    }
                }
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.i(TAG, "Screen OFF");
//                stopService();
                    if (packageName.equals("net.biyee.onviferenterprise")) {
                        Log.d(TAG, "Screen OFF Stop Service");
                        stopService();
                    }
                }
            }
        }
    };


    BroadcastReceiver mBRSdcard = new BroadcastReceiver() {
        @Override

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
//                sdcard_state.setBackgroundResource(R.drawable.sdcard_in);
//                sdcard_state.setEnabled(true);
                    sub_sdcard.setEnabled(true);

//                Log.d(TAG, "SD 카드 삽입");
                }
                if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
//                sdcard_state.setBackgroundResource(R.drawable.sdcard_out);
//                sdcard_state.setEnabled(false);
                    sub_sdcard.setEnabled(false);

//                Log.d(TAG, "SD 카드 제거");
                }
            }
        }
    };

    private void sdCard_check() throws IOException {
        Process process;
        Runtime runtime = Runtime.getRuntime();
        runtime.exec("su -c vdc volume mount /storage/extsd");

        try {
            Process p = Runtime.getRuntime().exec("su -c vdc volume list");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String sLine;
            while ((sLine = input.readLine()) != null) {
//                Log.d(TAG, "sLine: " + sLine);
                if (sLine.equals("110 0 extsd /storage/extsd 4") || sLine.equals("110 0 usbhost /storage/usbhost 4")) {
                    sub_sdcard.setEnabled(true);
                    return;
                } else {
                    sub_sdcard.setEnabled(false);

                }
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sdCard_unMount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.external_title));
        builder.setMessage(getResources().getString(R.string.external_body));
        builder.setPositiveButton(getResources().getString(R.string.no),
                (dialog, which) -> {
//                        Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_SHORT).show();
                });
        builder.setNegativeButton(getResources().getString(R.string.yes),
                (dialog, which) -> {
                    try {
                        Runtime.getRuntime().exec("su -c vdc volume unmount /storage/extsd");
                        Runtime.getRuntime().exec("su -c vdc volume unmount /storage/usbhost");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                        Toast.makeText(getApplicationContext(), "UnMount", Toast.LENGTH_SHORT).show();
                });
        builder.show();
    }

    private void doFirst() {
        SharedPreferences pref = getSharedPreferences("doFirst", MODE_PRIVATE);
        boolean doFirst = pref.getBoolean("doFirst", true);
        Log.v(TAG, "MainActivity doFirst: " + doFirst);
        if (doFirst) {
            Intent intent = new Intent(this, FactoryActivity.class);
            Log.v(TAG, "MainActivity doFirst startActivity()");
            startActivity(intent);

            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("doFirst", true);
            editor.apply();
        }

//        Log.d(TAG, "doFirst: " + doFirst);
    }

    @SuppressLint("WrongConstant")
    private void doSecond() {
        SharedPreferences pref = getSharedPreferences("doSecond", MODE_PRIVATE);
        boolean doSecond = pref.getBoolean("doSecond", true);
//        Log.d(TAG, "Dosecond start: " + doSecond);
//        int beforeColor;
//        int afterColor;
//        ConstraintLayout view = findViewById(R.id.main_view);
//        if(((ColorDrawable)view.getBackground()).getColor() == Color.parseColor("#1B2851")) {
//            beforeColor = Color.parseColor("#030000");
//            afterColor = Color.parseColor("#1B2851");
//        } else {
//            beforeColor = Color.parseColor("#1B2851");
//            afterColor = Color.parseColor("#030000");
//        }
//        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), beforeColor, afterColor);
//        colorAnimation.setDuration(500);
//        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                view.setBackgroundColor((int)animation.getAnimatedValue());
//            }
//        });
//        colorAnimation.start();


        if (doSecond) {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Ethernet/");
            if (dir.exists()) {
//                Log.d(TAG, "if Dir Exists: " + dir.exists());

                if (dir.isDirectory()) {

                    File[] files = dir.listFiles();

                    for (File file : files) {
                        if (file.delete()) {
                            Log.d(TAG, "File Delete: " + file.getName());
                        } else {
                            Log.d(TAG, "Not File Delete: " + file.getName());

                        }
                    }
                }
            } else {
                dir.mkdir();
                Log.d(TAG, "Dir Exists: " + dir.exists());

            }

            addFirstNetworkInfo("Link Local Address", "169.254.1.10", "255.255.0.0", "0.0.0.0", "168.126.63.1");
            addFirstNetworkInfo("IP 192.168.0.XXX", "192.168.0.150", "255.255.255.0", "192.168.0.1", "168.126.63.1");
            addFirstNetworkInfo("IP 192.168.1.XXX", "192.168.1.150", "255.255.255.0", "192.168.1.1", "168.126.63.1");

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setCancelable(false);
            dialog.setMessage(getResources().getString(R.string.factory_popup));
            dialog.setPositiveButton(getResources().getString(R.string.yes), (dialog1, which) -> runGuide());
            dialog.setNegativeButton(getResources().getString(R.string.no), (dialog12, which) -> dialog12.dismiss());
            AlertDialog alert = dialog.create();
            alert.show();
            StaticIpConfiguration mStaticIpConfiguration = new StaticIpConfiguration();
            EthernetManager mEthernetManager;
            mEthernetManager = (EthernetManager) getSystemService("ethernet");

            SettingsEthernet ethernet = new SettingsEthernet();
            ethernet.setDefaultIP(mStaticIpConfiguration, mEthernetManager);

            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("doSecond", false);
            editor.apply();
        }

    }

    public void addFirstNetworkInfo(String header, String ipAddr, String netMask, String gateWay, String dnsAddr) {
        String fileName = "Ethernet.txt";
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Ethernet/", fileName);


        FileOutputStream fos = null;
        BufferedWriter bufwr = null;

        String value[] = {header + "_@#@_" + ipAddr + "_@#@_" + netMask + "_@#@_" + gateWay + "_@#@_" + dnsAddr};
        saveArray(header, value);

        try {
            fos = new FileOutputStream(file, true);
            bufwr = new BufferedWriter(new OutputStreamWriter(fos));
            bufwr.write(header + "_@#@_" + ipAddr + "_@#@_" + netMask + "_@#@_" + gateWay + "_@#@_" + dnsAddr + "\n");
            bufwr.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (bufwr != null) {
                bufwr.close();
            }
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void saveArray(String arrayName, String[] array) {
//        Log.d(TAG, "Save: " + arrayName + " , " + Arrays.toString(array));
        SharedPreferences prefs = getSharedPreferences(NETWORK_PREFERENCE, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName + "_size", array.length);
        for (int i = 0; i < array.length; i++)
            editor.putString(arrayName + "_" + i, array[i]);
        editor.apply();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        // super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            doSecond();
        }
    }


    public void startWatchingViewerClose() {
//        Log.d(TAG, "startWatchingOEClose()");
        if (viewerReceiver == null) {
            viewerReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String state = intent.getStringExtra("state");
//                    Log.v(TAG, "startWatchingViewerClose Broadcast : " + state);
                    switch (state) {
                        case "resume":
                            break;
                        case "pause":
                            break;
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            String viewerBroadcast = "com.sscctv.seeeyesmonitor";
            filter.addAction(viewerBroadcast);
            registerReceiver(viewerReceiver, filter);
        }
    }

    public void stopWatchingViewerClose() {
        if (viewerReceiver != null) {
            unregisterReceiver(viewerReceiver);
        }
    }


    public void checkPoeState() {
//        Log.d(TAG, "startWatchingOEClose()");
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra("state");
//                Log.v(TAG, "Screen Broadcast : " + state);
                switch (state) {
                    case "PoE ON":
//                        ethSwitch.setChecked(true);
                        sendNotification();
                        poeStart();
                        break;
                    case "PoE OFF":
                        exitNotification();
//                        ethSwitch.setChecked(false);
                        poeStop();
                        break;
                    case "close":
                        stopService();
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        String closeBroadcast = "net.biyee.onviferenterprise.OnviferActivity";
        filter.addAction(closeBroadcast);
        registerReceiver(closeReceiver, filter);
    }

    public void sendNotification() {

//        Log.d(TAG, "sendNotification isRunning: " + isRunning);
        Resources res = getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("PoE")
                .setContentText("Running PoE service")
                .setTicker("PoE")
                .setSmallIcon(R.drawable.poe_noti)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.poe_large))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
//                    .setDefaults(Notification.DEFAULT_ALL);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_MESSAGE)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        nm.notify(11000, builder.build());

    }

    public void exitNotification() {
//        Log.d(TAG, "exitNotification: " + nm);

        if (nm != null) {
            nm.cancel(11000);
//            Log.d(TAG, "exitNotification cancel: " + nm);
        }

    }


    private void startService() {
        if (!catchValue) {
            catchValue = true;
            Intent poeIntent = new Intent(this, PoEIntentService.class);
            startService(poeIntent);
//            Log.d(TAG, "Start Catch Value = " + catchValue);
        }
    }

    private void stopService() {
        if (catchValue) {
            catchValue = false;
            Intent poeIntent = new Intent(this, PoEIntentService.class);
            stopService(poeIntent);
//            Log.d(TAG, "Stop Catch Value = " + catchValue);
        }
    }

    private void startStatusService() {
        Intent statusIntent = new Intent(this, statusService.class);
        startService(statusIntent);
//        Log.d(TAG, "Start Status Service");
    }

    private void stopStatusService() {
        Intent statusIntent = new Intent(this, statusService.class);
        stopService(statusIntent);
//        Log.d(TAG, "Start Status Service");
    }


    public void pseEnable() {
        // Select Lan port: CAM Side
        try {
            opt.writeBytes("echo 1 > /sys/class/gpio_sw/PE17/data\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pseDisable() {
        // Select Lan port: NVR Side
        try {
            opt.writeBytes("echo 0 > /sys/class/gpio_sw/PE17/data\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void vpEnable() {
        // VP Pin Select: IPM internal 48V output
        try {
            opt.writeBytes("echo 1 > /sys/class/gpio_sw/PE11/data\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void vpDisable() {
//        Log.d(TAG, "Vp disable");
        // VP Pin Select: IPM external 48V output
        try {
            opt.writeBytes("echo 0 > /sys/class/gpio_sw/PE11/data\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void poeStart() {
//        Log.d(TAG, "PoE Start");
//        vpDisable();
        pseEnable();
    }        // PoE Start

    public void poeStop() {
//        vpEnable();
        pseDisable();
    }         // PoE Stop

    public String getPseState() {
        String sValue = "";

        try {
            Process p = Runtime.getRuntime().exec("cat /sys/class/gpio_sw/PE17/data");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            sValue = input.readLine();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sValue;
    }

    public class IsNetworkReceiver extends BroadcastReceiver {

        private Activity act;

        public IsNetworkReceiver() {
            super();
        }

        public IsNetworkReceiver(Activity activity) {
            this.act = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            ImageView wifi_state = findViewById(R.id.wifi_state);
//            wifi_state.setImageResource(R.drawable.wifi_out);
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
//                Log.d(TAG, "What??");
                ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetInfo = conManager.getActiveNetworkInfo();
//                try {
//                    if (activeNetInfo.getTypeName() != null) {
//                        if (activeNetInfo.getTypeName().equals("WIFI")) {
//                            if (isWifi()) {
//                                Log.d(TAG, "WiFi OK");
                //TODO
//                            }
//                        }
//                    } else {
                //TODO
//                        wifi_state.setImageResource(R.drawable.wifi_out);
//                        Log.d(TAG, "WiFi Fail");
//                    }
//                } catch (Exception e) {
//                    wifi_state.setImageResource(R.drawable.wifi_out);
//                    Log.d(TAG, "3G Fail");
//                }
//            } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
//                if (isWifi()) {
//                    Log.d(TAG, "RSSI CHANGE");
//                } else {
//                    Log.d(TAG, "RSSI NOT");

//                }
            }
        }

        // wifi 사용가능
        public boolean isWifi() {
            boolean result = false;
            WifiManager wm;
            wm = (WifiManager) act.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//            ImageView wifi_state = findViewById(R.id.wifi_state);
            WifiInfo wInfo = wm.getConnectionInfo();
            if (wInfo != null) {
                // 연결상태 확인
                NetworkInfo.DetailedState ni_ds = WifiInfo.getDetailedStateOf(wInfo.getSupplicantState());
                if ((wInfo.getIpAddress() > 0 && wInfo.getSSID() != null && wInfo.getSupplicantState().toString().equals("COMPLETED"))
                        && (ni_ds == NetworkInfo.DetailedState.CONNECTED || ni_ds == NetworkInfo.DetailedState.OBTAINING_IPADDR)
                ) {
                    // RSSI 는 -100에 가까울수록 안좋고 0에 가까울수록 좋음
//                    if (wInfo.getRssi() < -80 && wInfo.getRssi() > -100) {
//                        wifi_state.setImageResource(R.drawable.wifi_in_1);
//                        Log.e(TAG, "WiFi RSSI 1 = " + wInfo.getRssi());
//                    } else if (wInfo.getRssi() < -50 && wInfo.getRssi() > -79) {
//                        wifi_state.setImageResource(R.drawable.wifi_in_2);
//                        Log.e(TAG, "WiFi RSSI 2 = " + wInfo.getRssi());

//                    } else if (wInfo.getRssi() < -30 && wInfo.getRssi() > -49) {
//                        wifi_state.setImageResource(R.drawable.wifi_in_3);
//                        Log.e(TAG, "WiFi RSSI 3 = " + wInfo.getRssi());

//                    } else if (wInfo.getRssi() < -0 && wInfo.getRssi() > -29) {
//                        wifi_state.setImageResource(R.drawable.wifi_in_4);
//                        Log.e(TAG, "WiFi RSSI 4 = " + wInfo.getRssi());
//                    }

                    result = true;
                }
            }
            return result;
        }


    }

    public void isEthernet() {
        ConnectivityManager manager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ethernet = manager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        NetworkInfo active = manager.getActiveNetworkInfo();
//        Log.d(TAG, "Network Active: " + active);
//        ImageView eth_state = findViewById(R.id.eth_state);
        if (ethernet != null) {
            SharedPreferences sharedPreferences = getSharedPreferences("mode", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (ethernet.isConnected()) {
//                Log.d(TAG, "Ethernet Connect");
                editor.putString("stat", "on");
//                eth_state.setImageResource(R.drawable.eth_in);
            } else {
//                Log.d(TAG, "Not Connect");
                editor.putString("stat", "off");
//                eth_state.setImageResource(R.drawable.eth_out);
            }
            editor.apply();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        Log.d(TAG, "KeyCode: " + keyCode + " event: " + event);

        return runState;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.d(TAG, "onKeyDown: " +keyCode + " KeyEvent: " + event);

        return runState;
    }


    @Override
    public void onBackPressed() {
//        Log.d(TAG, "Back Pressed");
//        if (!getIntent().hasCategory(Intent.CATEGORY_HOME)) {
//            super.onBackPressed();
//        }
    }

    private void buildTimeStamp() {
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        SimpleDateFormat clsFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        Log.w(TAG, "Build Date: " + clsFormat.format(buildDate));
    }

}






