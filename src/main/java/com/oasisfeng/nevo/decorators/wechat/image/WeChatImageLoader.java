package com.oasisfeng.nevo.decorators.wechat.image;

import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.util.AbstractMap;

import androidx.annotation.WorkerThread;
import java9.util.Comparators;
import java9.util.stream.Stream;

/**
 * Created by Oasis on 2018-8-7.
 * Modify by Kr328 on 2019-1-1
 */
public class WeChatImageLoader {
	private static final long MAX_TIME_DIFF = 8000;

	private WeChatImageDirectoryObserver directoryObserver = new WeChatImageDirectoryObserver();

	boolean tryInitialize() {
		return directoryObserver.tryInitialize();
	}

	@WorkerThread
	File loadImage(long time) {
		AbstractMap.SimpleEntry<Long ,File> result = FilesUtils.listAllFrom(
							Stream.of(directoryObserver.getAccountDirectories().toArray(new String[0]))
									.map(s -> new File(s + "/image2")).toArray(File[]::new)
				)
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
}
