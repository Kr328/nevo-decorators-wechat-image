package com.oasisfeng.nevo.decorators.wechat.image;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

public class WeChatImageUtils {
    private static List<String> sPlaceholders;

    static synchronized boolean isImagePlaceholder(final Context context, final String text) {
        final int pos_colon = text.indexOf(':');
        final String message = (pos_colon > 0 ? text.substring(pos_colon + 1) : text).trim();	// Remove possible unread counter and sender name
        if (sPlaceholders == null) sPlaceholders = Arrays.asList(context.getResources().getStringArray(R.array.text_placeholders_for_picture));
        return sPlaceholders.contains(message);	// Search throughout languages since WeChat can be configured to locale other than current system locale.
    }
}
