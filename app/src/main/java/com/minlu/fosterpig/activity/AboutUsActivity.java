package com.minlu.fosterpig.activity;

import android.view.View;

import com.minlu.fosterpig.R;
import com.minlu.fosterpig.base.BaseActivity;

/**
 * Created by user on 2016/11/28.
 */
public class AboutUsActivity extends BaseActivity {
    @Override
    public void onCreateContent() {

        getThreeLine().setVisibility(View.GONE);
        setBackVisibility(View.VISIBLE);
        setSettingVisibility(View.GONE);

        getBaseTitle().setText("关于我们");

        setContent(R.layout.activity_about_us);

    }
}
