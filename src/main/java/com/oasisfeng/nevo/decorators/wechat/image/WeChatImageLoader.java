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
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import java9.util.Comparators;
import java9.util.stream.Stream;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by Oasis on 2018-8-7.
 */
public class WeChatImageLoader {
	static boolean isImagePlaceholder(final Context context, final String text) {
		if (sPlaceholders == null) sPlaceholders = Arrays.asList(context.getResources().getStringArray(R.array.text_placeholders_for_picture));
		return sPlaceholders.contains(text);	// Search throughout languages since WeChat can be configured to different language than current system language.
	}

	@SuppressLint("MissingPermission")
	@RequiresPermission(READ_EXTERNAL_STORAGE)
	File loadImage() {
		if (mAccountRootPath == null) return null;
		final long now = System.currentTimeMillis();

		return Stream.of(Files.walkFiles(mAccountRootPath))
				.filter(File::isFile)
				.filter(file -> file.getName().startsWith("th_"))
				.map(file -> new AbstractMap.SimpleEntry<>(now - file.lastModified(), file))
				.filter(entry -> 0 < entry.getKey() && entry.getKey() < MAX_TIME_DIFF)
				.min(Comparators.comparing(AbstractMap.SimpleEntry::getKey))
				.get().getValue();
	}

	WeChatImageLoader() {
		File root = null;
		final File[] files = WECHAT_PATH.listFiles();
		if (files != null) for (final File file : files) {
			if (file.getName().length() != 32) continue;		// All account paths are 32 hex chars.
			if (root == null || file.lastModified() > root.lastModified()) root = file;
		}
		if (root == null) {
			mAccountRootPath = null;
			Log.e(TAG, "No account path (32 hex chars) found in " + WECHAT_PATH);
		} else mAccountRootPath = new File(root, "image2");
	}

	static PendingIntent buildPermissionRequest(final Context context) {
		return PendingIntent.getActivity(context, 0, new Intent(context, WeChatImageLoader.PermissionRequestActivity.class), FLAG_UPDATE_CURRENT);
	}

	private static final long MAX_TIME_DIFF = 2000;
	private static final File WECHAT_PATH = new File(Environment.getExternalStorageDirectory(), "/Tencent/MicroMsg");

	private final @Nullable File mAccountRootPath;
	private static List<String> sPlaceholders;
	private static final String TAG = "Nevo.WeChatPic";

	public static class PermissionRequestActivity extends Activity {

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
