package com.dcco.app.iSilo;

import android.app.Application;
import com.dcco.app.iSilo.state.AppState;

public class iSiloApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppState.appContext = this;
        AppState.sdkVersion = android.os.Build.VERSION.SDK_INT;
    }
}
