package com.app.sdcardscanner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ResultActivity extends AppCompatActivity {

    @BindView(R.id.top10)
    TextView top10;
    @BindView(R.id.list_top_10)
    TextView listTop10;
    @BindView(R.id.average)
    TextView average;
    @BindView(R.id.average_file)
    TextView averageFileSize;
    @BindView(R.id.top5)
    TextView top5;
    @BindView(R.id.list_top_5)
    TextView top5Frequent;
    private String avg;
    private String top;
    private String freTop;
    private SharedPreferences sharedPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        ButterKnife.bind(this);
        setViews();
    }

    private void setViews() {
        sharedPreference = getSharedPreferences(FileScannerMainActivity.PREFERENCES, MODE_PRIVATE);

        if (sharedPreference.contains(FileScannerMainActivity.AVGFILESIZE)) {
            avg = sharedPreference.getString(FileScannerMainActivity.AVGFILESIZE, "");
        }
        if (sharedPreference.contains(FileScannerMainActivity.TENBIG)) {
            top = sharedPreference.getString(FileScannerMainActivity.TENBIG, "");
        }
        if (sharedPreference.contains(FileScannerMainActivity.EXTFREQUENT)) {
            freTop = sharedPreference.getString(FileScannerMainActivity.EXTFREQUENT, "");
        }

        listTop10.setText(top);
        averageFileSize.setText(avg);
        top5Frequent.setText(freTop);
    }

}

