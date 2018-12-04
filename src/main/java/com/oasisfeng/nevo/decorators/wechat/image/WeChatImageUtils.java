package com.oasisfeng.nevo.decorators.wechat.image;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

public class WeChatImageUtils {
    private static List<String> sPlaceholders;

    static synchronized boolean isImagePlaceholder(final Context context, final String text) {
        if (sPlaceholders == null) sPlaceholders = Arrays.asList(context.getResources().getStringArray(R.array.text_placeholders_for_picture));
        return sPlaceholders.contains(text);	// Search throughout languages since WeChat can be configured to different language than current system language.
    }
}
