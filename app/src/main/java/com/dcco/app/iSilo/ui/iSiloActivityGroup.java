package com.dcco.app.iSilo.ui;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import com.dcco.app.iSilo.state.AppState;
import com.dcco.app.iSilo.ui.doclist.DocListActivity;
import com.dcco.app.iSilo.ui.reader.ReadActivity;

public class iSiloActivityGroup extends ActivityGroup {

    public static final String VIEW = "RV";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState.activityContext = this;
        AppState.activityGroup = this;

        startDocList();
    }

    private void startDocList() {
        Intent intent = new Intent(this, DocListActivity.class);
        startActivity(intent);
        finish();
    }

    public void startReader(String filePath) {
        Intent intent = new Intent(this, ReadActivity.class);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
    }
}
