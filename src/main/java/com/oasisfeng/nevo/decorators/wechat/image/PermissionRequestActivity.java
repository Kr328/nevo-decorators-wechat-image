package com.oasisfeng.nevo.decorators.wechat.image;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class PermissionRequestActivity extends Activity {
    public static PendingIntent buildPermissionRequest(final Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, PermissionRequestActivity.class), FLAG_UPDATE_CURRENT);
    }

    @Override protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !getSystemService(UserManager.class).isUserUnlocked())
            return;

        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), getText(R.string.toast_image_preview_activated), Toast.LENGTH_LONG).show();
            finish();
        } else requestPermissions(new String[] { READ_EXTERNAL_STORAGE }, 0);
    }

    @Override public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        if (grantResults.length == 1 && READ_EXTERNAL_STORAGE.equals(permissions[0]) && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(getApplicationContext(), getText(R.string.toast_image_preview_activated), Toast.LENGTH_LONG).show();
        finish();
    }
}
