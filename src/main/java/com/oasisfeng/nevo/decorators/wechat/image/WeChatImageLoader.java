package com.oasisfeng.nevo.decorators.wechat.image;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.AbstractMap;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import java9.util.Comparators;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by Oasis on 2018-8-7.
 * Modify by Kr328 on 2019-1-1
 */
public class WeChatImageLoader {
	@WorkerThread
	File loadImage(long time) {
		File accountRoot = findAccountRootDirectory();
		if ( accountRoot == null ) {
			Log.e(TAG, "No account path (32 hex chars) found in " + WeChatImageDirectoryObserver.WECHAT_PATH);
			return null;
		}

		AbstractMap.SimpleEntry<Long ,File> result = FilesUtils.listEntry(new File(accountRoot ,"image2"))
				.filter(File::isFile)
				.filter(file -> file.getName().startsWith("th_") || file.getName().endsWith(".jpg"))
				.map(file -> new AbstractMap.SimpleEntry<>(time - file.lastModified(), file)) //Key: now - lastModified ,Value: File
				//.peek(file -> Log.i(TAG ,file.toString())
				.filter(entry -> 0 < entry.getKey() && entry.getKey() < MAX_TIME_DIFF)
				.min(Comparators.comparing(AbstractMap.SimpleEntry::getKey))
				.orElseGet(() -> null);

		if ( result == null )
			return null;
		return result.getValue();
	}

	private File findAccountRootDirectory() {
		File[] files         = WeChatImageDirectoryObserver.WECHAT_PATH.listFiles();
		long   last_modified = 0;
		File   result        = null;

		if ( files == null )
			return null;

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
