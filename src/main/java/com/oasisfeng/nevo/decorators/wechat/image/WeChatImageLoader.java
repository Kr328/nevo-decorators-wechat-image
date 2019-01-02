package com.oasisfeng.nevo.decorators.wechat.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.AbstractMap;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.annotation.WorkerThread;
import java9.util.Comparators;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by Oasis on 2018-8-7.
 */
public class WeChatImageLoader {
	@WorkerThread
	@SuppressLint("MissingPermission")
	@RequiresPermission(READ_EXTERNAL_STORAGE)
	static File loadImage() {
		File accountRoot = findAccountRootDirectory();
		if ( accountRoot == null ) {
			Log.e(TAG, "No account path (32 hex chars) found in " + WECHAT_PATH);
			return null;
		}

		long now = System.currentTimeMillis();

		AbstractMap.SimpleEntry<Long ,File> result = FilesUtils.listEntry(new File(accountRoot ,"image2"))
				.filter(File::isFile)
				.filter(file -> file.getName().startsWith("th_") || file.getName().endsWith(".jpg"))
				.map(file -> new AbstractMap.SimpleEntry<>(now - file.lastModified(), file)) //Key: now - lastModified ,Value: File
				//.peek(file -> Log.i(TAG ,file.toString())
				.filter(entry -> 0 < entry.getKey() && entry.getKey() < MAX_TIME_DIFF)
				.min(Comparators.comparing(AbstractMap.SimpleEntry::getKey))
				.orElseGet(() -> null);

		if ( result == null )
			return null;
		return result.getValue();
	}

	private static File findAccountRootDirectory() {
		File[] files         = WECHAT_PATH.listFiles();
		long   last_modified = 0;
		File   result        = null;

		for ( File f : files ) {
			if ( f.getName().length() != 32 )
				continue;
			if ( f.lastModified() > last_modified ) {
				last_modified = f.lastModified();
				result = f;
			}
		}

		return result;
	}

	private static final long MAX_TIME_DIFF = 10000;
	private static final File WECHAT_PATH = new File(Environment.getExternalStorageDirectory(), "/Tencent/MicroMsg");

	private static final String TAG = "Nevo.WeChatPic";

	public static class PermissionRequestActivity extends Activity {
		public static PendingIntent buildPermissionRequest(final Context context) {
			return PendingIntent.getActivity(context, 0, new Intent(context, WeChatImageLoader.PermissionRequestActivity.class), FLAG_UPDATE_CURRENT);
		}

		@Override protected void onResume() {
			super.onResume();
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
}
