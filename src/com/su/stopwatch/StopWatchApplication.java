package com.su.stopwatch;

import com.su.stopwatch.data.DataModel;

import android.app.Application;

public class StopWatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DataModel.getDataModel().setContext(getApplicationContext());
    }
}
