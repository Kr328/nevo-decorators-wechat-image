package com.oasisfeng.nevo.decorators.wechat.image;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.oasisfeng.nevo.sdk.MutableNotification;
import com.oasisfeng.nevo.sdk.MutableStatusBarNotification;
import com.oasisfeng.nevo.sdk.NevoDecoratorService;

import java.io.File;
import java.util.HashMap;

import androidx.core.content.FileProvider;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.P;

/**
 * Created by Oasis on 2018-11-30.
 */
public class WeChatImagePreviewDecorator extends NevoDecoratorService {
	private static final String NEVO_PACKAGE = "com.oasisfeng.nevo";

	private static final String KEY_DATA_MIME_TYPE = "type";
	private static final String KEY_DATA_URI= "uri";

	@Override protected void apply(final MutableStatusBarNotification evolving) {
		final MutableNotification n = evolving.getNotification();
		final CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT);

		if (text == null) return;
		if (!WeChatImageUtils.isImagePlaceholder(this, text.toString())) return;
		if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
			n.addAction(new Notification.Action.Builder(null, getText(R.string.action_preview_image),
					WeChatImageLoader.PermissionRequestActivity.buildPermissionRequest(this)).build());
			return;
		}

		final File image = WeChatImageLoader.loadImage();
		if (image == null) return;

		Log.i(TAG ,"Image Load " + image);

		@SuppressLint("InlinedApi") final Parcelable[] messages = n.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
		if (SDK_INT >= P && messages != null && messages.length > 0) {
			final Object last = messages[messages.length - 1];
			if (! (last instanceof Bundle)) return;
			final Bundle last_message = (Bundle) last;
			if (last_message.containsKey(KEY_DATA_MIME_TYPE) && last_message.containsKey(KEY_DATA_URI)) return;
			Uri uri = FileProvider.getUriForFile(this ,BuildConfig.APPLICATION_ID + ".wechat_root" ,image);
			Log.i(TAG ,"Uri = " + uri);
			last_message.putString(KEY_DATA_MIME_TYPE, "image/jpeg");
			last_message.putParcelable(KEY_DATA_URI, uri);	// TODO: Keep image mapping for previous messages.
			grantNevoUriReadPermission(evolving.getKey() ,uri);
			n.extras.putParcelableArray(Notification.EXTRA_MESSAGES, messages.clone()); // Use clone to tell the SDK it is actually changed.
		} else if (messages == null || messages.length == 1) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = SDK_INT >= O ? Bitmap.Config.HARDWARE : Bitmap.Config.ARGB_8888;
			n.extras.putString(Notification.EXTRA_TEMPLATE, TEMPLATE_BIG_PICTURE);
			n.extras.putParcelable(Notification.EXTRA_PICTURE, BitmapFactory.decodeFile(image.getPath(), options));
			n.extras.putCharSequence(Notification.EXTRA_SUMMARY_TEXT, text);
		}
	}

	@Override
	protected void onNotificationRemoved(String key, int reason) {
		revokeUriReadPermission(key);
	}

	private void grantNevoUriReadPermission(String key , Uri uri) {
		revokeUriReadPermission(key);
		grantUriPermission(NEVO_PACKAGE ,uri ,Intent.FLAG_GRANT_READ_URI_PERMISSION);
		grantedUriMap.put(key, uri);
	}

	private void revokeUriReadPermission(String key) {
		revokeUriPermission(grantedUriMap.remove(key) ,Intent.FLAG_GRANT_READ_URI_PERMISSION);
	}

	private HashMap<String ,Uri> grantedUriMap = new HashMap<>();
}
