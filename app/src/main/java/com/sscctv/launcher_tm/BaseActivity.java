package com.sscctv.launcher_tm;

import android.support.v7.app.AppCompatActivity;

import me.toptas.fancyshowcase.FancyShowCaseView;

/**
 * Created by yeldos on 3/29/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        if (FancyShowCaseView.isVisible(this)) {
            FancyShowCaseView.hideCurrent(this);
        } else {
            super.onBackPressed();
        }
    }
}
