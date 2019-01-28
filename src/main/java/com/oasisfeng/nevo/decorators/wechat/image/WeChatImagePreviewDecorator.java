package com.oasisfeng.nevo.decorators.wechat.image;

import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
//import android.util.Log;

import com.oasisfeng.nevo.sdk.MutableNotification;
import com.oasisfeng.nevo.sdk.MutableStatusBarNotification;
import com.oasisfeng.nevo.sdk.NevoDecoratorService;

import java.io.File;
import java.util.HashMap;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.P;

/**
 * Created by Oasis on 2018-11-30.
 * Modify by Kr328 on 2019-1-5
 */

public class WeChatImagePreviewDecorator extends NevoDecoratorService {
	private static final String NEVO_PACKAGE_NAME = "com.oasisfeng.nevo";
	private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
	private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

	private static final String KEY_DATA_MIME_TYPE = "type";
	private static final String KEY_DATA_URI= "uri";

	@Override protected void apply(final MutableStatusBarNotification evolving) {
		final MutableNotification n = evolving.getNotification();
		final CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT);

		if (text == null) return;
		if (!WeChatImageUtils.isImagePlaceholder(this, text.toString())) return;
		if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
			n.addAction(new Notification.Action.Builder(null, getText(R.string.action_preview_image),
					PermissionRequestActivity.buildPermissionRequest(this)).build());
			return;
		}

		if ((SDK_INT >= N && handleMessagingStyle(evolving.getKey() ,n)) || handleBigPicture(evolving.getKey() ,n) )
			Log.i(Global.TAG ,"Applied " + evolving.getKey());
		else
			Log.w(Global.TAG ,"Unsupported notification " + evolving.getNotification());
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	private boolean handleMessagingStyle(String key , MutableNotification notification) {
		Parcelable[] messages = notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
		if ( SDK_INT < P || messages == null || messages.length < 1 ) return false;
		int index = messages.length - 1;
		Object last_message = messages[index];
		if (!(last_message instanceof Bundle)) return false;
		File image = imageStack.getImageFileForKey(key ,index);

		if (image != null) {
			Uri uri = FileProvider.getUriForFile(this ,
					BuildConfig.APPLICATION_ID + ".wechat_root" ,
					image);

			Log.i(Global.TAG ,"Loaded " + uri);

			if ( applyMessageStyle((Bundle) last_message ,uri ,key) )
				notification.extras.putParcelableArray(Notification.EXTRA_MESSAGES ,messages.clone());
		} else
			imageStack.postLoadImage(key ,index ,notification.when);

		return true;
	}

	private boolean handleBigPicture(String key ,MutableNotification n) {
		File image = imageStack.getImageFileForKey(key ,0);

		if ( image == null )
			imageStack.postLoadImage(key ,0 ,n.when);
		else
			applyNormal(n ,n.extras.getCharSequence(Notification.EXTRA_TEXT) ,image);

		return true;
	}

	private boolean applyMessageStyle(Bundle data ,Uri uri ,String key) {
		if (data.containsKey(KEY_DATA_MIME_TYPE) && data.containsKey(KEY_DATA_URI)) return false;
		grantUriReadPermission(key ,uri);
		data.putString(KEY_DATA_MIME_TYPE, "image/jpeg");
		data.putParcelable(KEY_DATA_URI, uri);
		return true;
	}

	private void applyNormal(MutableNotification n ,CharSequence text ,File image) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = SDK_INT >= O ? Bitmap.Config.HARDWARE : Bitmap.Config.ARGB_8888;
		n.extras.putString(Notification.EXTRA_TEMPLATE, TEMPLATE_BIG_PICTURE);
		n.extras.putParcelable(Notification.EXTRA_PICTURE, BitmapFactory.decodeFile(image.getPath(), options));
		n.extras.putCharSequence(Notification.EXTRA_SUMMARY_TEXT, text);
	}

	@Override
	protected void onNotificationRemoved(String key, int reason) {
		revokeUriReadPermission(key);
		imageStack.removeKey(key);
	}

	@Override
	protected void onConnected() {
		if ( !imageStack.isAlive() )
			imageStack.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		imageStack.interrupt();
	}

	private void grantUriReadPermission(String key , Uri uri) {
		revokeUriReadPermission(key);
		grantUriPermission(NEVO_PACKAGE_NAME ,uri ,Intent.FLAG_GRANT_READ_URI_PERMISSION);
		grantUriPermission(WECHAT_PACKAGE_NAME ,uri ,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        grantUriPermission(SYSTEM_UI_PACKAGE_NAME ,uri ,Intent.FLAG_GRANT_READ_URI_PERMISSION);
		grantedUriMap.put(key, uri);
	}

	private void revokeUriReadPermission(String key) {
		revokeUriPermission(grantedUriMap.remove(key) ,Intent.FLAG_GRANT_READ_URI_PERMISSION);
	}

	private HashMap<String ,Uri> grantedUriMap = new HashMap<>();
	private WeChatImageStack imageStack = new WeChatImageStack((key ,id) ->
			recastNotification(key ,null));
}
