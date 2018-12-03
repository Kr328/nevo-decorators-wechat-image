package com.oasisfeng.nevo.decorators.wechat.image;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class WeChatImagePreviewDecoratorSettingsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context ,"Unsupported" ,Toast.LENGTH_LONG).show();
    }
}
