package com.oasisfeng.nevo.decorators.wechat.image;

import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.util.AbstractMap;
import java.util.regex.Pattern;

import androidx.annotation.WorkerThread;
import java9.util.Comparators;

/**
 * Created by Oasis on 2018-8-7.
 * Modify by Kr328 on 2019-1-1
 */
public class WeChatImageLoader {
	private static final long MAX_TIME_DIFF = 8000;
	private static String currentImageDirectory = "";

	private WeChatImageDirectoryObserver directoryObserver = new WeChatImageDirectoryObserver(this::onImageDirectoryChanged);

	boolean tryInitialize() {
		return directoryObserver.tryInitialize();
	}

	@WorkerThread
	File loadImage(long time ,String directory) {
		Log.i(Global.TAG ,"Try load from " + directory + " directoryObserver " + directoryObserver.hasObserver());

		AbstractMap.SimpleEntry<Long ,File> result = FilesUtils.listEntry(new File(directory))
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

	synchronized String getCurrentDirectory() {
		return currentImageDirectory;
	}

	private synchronized void onImageDirectoryChanged(int event ,String path) {
		if ( event == FileObserver.CREATE && new File(path).isDirectory() )
			currentImageDirectory = path;
		Log.i(Global.TAG ,"Event " + path);
	}
}
