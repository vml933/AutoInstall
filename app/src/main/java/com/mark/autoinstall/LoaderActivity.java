package com.mark.autoinstall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class LoaderActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);

        //因為必須在Manifest備註<category android:name="com.htc.intent.category.VRAPP" />，收藏庫才會出現app的icon，但app會進入vrMode，無法控制，故再跳轉一次
        ToNextActivity();
    }

    private void ToNextActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
