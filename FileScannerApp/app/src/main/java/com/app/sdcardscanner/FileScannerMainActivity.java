package com.app.sdcardscanner;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.app.sdcardscanner.service.ScanIntentService;
import com.dinuscxj.progressbar.CircleProgressBar;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FileScannerMainActivity extends AppCompatActivity {
    public static final String PREFERENCES = "Prefs";
    public static final String AVGFILESIZE = "avgFileSize";
    public static final String EXTFREQUENT = "extFrequent";
    public static final String TENBIG = "tenBig";
    private static final String TAG = FileScannerMainActivity.class.getName();
    private static final int REQUEST_READ_STORAGE = 1;
    public SharedPreferences sharedPreferences;
    public String tenBig;
    public String avgFileSize;
    public String extFrequent;
    @BindView(R.id.tv_retrieving)
    TextView tv_retrieving;
    @BindView(R.id.btn_start_scan)
    Button startButton;
    @BindView(R.id.btn_stop_scan)
    Button stopButton;
    @BindView(R.id.btn_show_result)
    Button show_result;
    @BindView(R.id.circleProgressBar)
    CircleProgressBar mProgressBar;
    MenuItem shareOption;
    private Intent intent, nIntent, mShareIntent;
    private PendingIntent pIntent;
    private Notification noti;
    private NotificationManager nm;
    private ShareActionProvider mShareActionProvider;
    private int visible = 0;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            startButton.setEnabled(true);
            if (bundle != null) {
                tenBig = bundle.getString(ScanIntentService.TOP10);
                extFrequent = bundle.getString(ScanIntentService.FREQUENTEXT);
                avgFileSize = bundle.getString(ScanIntentService.AVERAGE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(TENBIG, tenBig);
                editor.putString(EXTFREQUENT, extFrequent);
                editor.putString(AVGFILESIZE, avgFileSize);
                editor.apply();
                visible = 1;
                tv_retrieving.setVisibility(View.GONE);
                disableAndEnableButton(stopButton, startButton);
                show_result.setVisibility(View.VISIBLE);
                shareOption.setVisible(true);
                mShareActionProvider.setShareIntent(createSharedIntent());
                setNotification("SD CARD Scan", "Scan Ended");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_scanner_main);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            visible = savedInstanceState.getInt("vis");
            if (visible == 0) {
                show_result.setVisibility(View.VISIBLE);
            } else {
                show_result.setVisibility(View.INVISIBLE);
            }
        }
        sharedPreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (isStoragePermissionGranted()) {
            intent = new Intent(this, ScanIntentService.class);
        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerReceiver(receiver, new IntentFilter(ScanIntentService.NOTIFICATION));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("vis", show_result.getVisibility());
    }

    @OnClick(R.id.btn_start_scan)
    public void startScan(View view) {

        if (intent != null) {
            tv_retrieving.setVisibility(View.VISIBLE);
            disableAndEnableButton(startButton, stopButton);
            setNotification("SD Card Scanner", "Scan Started");
            setHandler();
            startService(intent);
        } else {
            Toast.makeText(this, "Please grant permissions to read files", Toast.LENGTH_LONG).show();

        }

    }

    private void setHandler() {
        final MyHandler mHandler = new MyHandler(this);
        intent.putExtra("messenger", new Messenger(mHandler));
    }

    private void setNotification(String contentTitle, String contentText) {
        nIntent = new Intent();
        pIntent = PendingIntent.getActivity(FileScannerMainActivity.this, 0, nIntent, 0);
        noti = new Notification.Builder(FileScannerMainActivity.this)
                .setTicker("Title")
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent).getNotification();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert nm != null;
        nm.notify(0, noti);
    }

    private void disableAndEnableButton(Button btnToDisable, Button btnToEnable) {
        btnToDisable.setEnabled(false);
        btnToDisable.setBackgroundColor(getResources().getColor(R.color.primary_light));
        btnToEnable.setEnabled(true);
        btnToEnable.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }

    @OnClick(R.id.btn_stop_scan)
    public void stopScan(View view) {
        startButton.setEnabled(true);
        startButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        stopButton.setEnabled(false);
        stopButton.setBackgroundColor(getResources().getColor(R.color.primary_light));
        tv_retrieving.setVisibility(View.GONE);
        setNotification("SD CARD Scan", "Scan Stoped forcefully");
        stopService(intent);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQUEST_READ_STORAGE);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }

    }

    @OnClick(R.id.btn_show_result)
    public void showResults(View view) {
        Intent resultActivityIntent = new Intent(getBaseContext(), ResultActivity.class);
        startActivity(resultActivityIntent);
    }

    private Intent createSharedIntent() {
        mShareIntent = new Intent(Intent.ACTION_SEND);
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        String share = "Top Ten file sizes with Name: \n" + tenBig + "\n Average File Size: \n" + avgFileSize
                + "\n Top five extensions with Frequency: \n" + extFrequent;
        Log.d(TAG, "createSharedIntent: " + share);
        mShareIntent.putExtra(Intent.EXTRA_TEXT, share);
        return mShareIntent;
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startButton.setEnabled(true);
        stopService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    intent = new Intent(this, ScanIntentService.class);
                } else {
                    Toast.makeText(FileScannerMainActivity.this, "The app was not allowed to read from your storage"
                            + ". "
                            + "Hence, it cannot "
                            + "function properly. Please consider granting it this permission", Toast.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        // Locate MenuItem with ShareActionProvider
        shareOption = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareOption);
        // Return true to display menu
        return true;
    }

    /***
     * Handler for updating progress bar
     */
    private static class MyHandler extends Handler {
        private final WeakReference<FileScannerMainActivity> mActivity;

        public MyHandler(FileScannerMainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FileScannerMainActivity activity = mActivity.get();
            if (activity != null) {
                Bundle reply = msg.getData();
                if (reply.get("percentage").toString() != null) {
                    int percentage = Integer.parseInt(reply.get("percentage").toString());
                    ((CircleProgressBar) activity.findViewById(R.id.circleProgressBar)).setProgress(percentage);
                }
            }
        }
    }

}



