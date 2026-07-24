package com.dcco.app.iSilo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, iSiloActivityGroup.class);
        startActivity(intent);
        finish();
    }
}
