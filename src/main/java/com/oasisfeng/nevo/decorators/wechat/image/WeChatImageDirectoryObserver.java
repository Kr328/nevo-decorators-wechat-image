package com.oasisfeng.nevo.decorators.wechat.image;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.util.Hashtable;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

public class WeChatImageDirectoryObserver {
    private static final File WECHAT_PATH = new File(Environment.getExternalStorageDirectory(), "/Tencent/MicroMsg");
    private static final Pattern PATTERN_WECHAT_ACCOUNT_DIRECTORY = Pattern.compile("[0-9a-fA-F]{32}");
    private static final Pattern PATTERN_WECHAT_IMAGE_DIRECTORY = Pattern.compile("[0-9a-fA-F]{2}");

    private FileObserver accountRootObserver;
    private Hashtable<String ,FileObserver> accountObservers = new Hashtable<>();
    private Callback callback;

    public WeChatImageDirectoryObserver(Callback callback) {
        this.callback = callback;
    }

    public synchronized boolean tryInitialize() {
        if ( accountRootObserver != null )
            return true;

        if ( WECHAT_PATH.exists() ) {
            accountRootObserver = new FileObserver(WECHAT_PATH.getAbsolutePath(), FileObserver.CREATE | FileObserver.DELETE | FileObserver.DELETE_SELF) {
                @Override
                public void onEvent(int event, @Nullable String path) {
                    event &= CREATE | DELETE | DELETE_SELF;

                    switch (event) {
                        case CREATE:
                            if (PATTERN_WECHAT_ACCOUNT_DIRECTORY.matcher(path).matches())
                                handleAccountCreated(path);
                            break;
                        case DELETE:
                            if (PATTERN_WECHAT_ACCOUNT_DIRECTORY.matcher(path).matches())
                                handleAccountDeleted(path);
                            break;
                        case DELETE_SELF:
                            handleDeletedSelf();
                            break;
                    }
                }
            };

            accountRootObserver.startWatching();

            searchAndPutAccount();
        }

        return true;
    }

    public boolean hasObserver() {
        return !accountObservers.isEmpty();
    }

    private void handleAccountCreated(String accountId) {
        Log.i(Global.TAG ,"AccountCreated " + accountId);
        FileObserver observer = new FileObserver(new File(WECHAT_PATH ,accountId + "/image2").getAbsolutePath() ,FileObserver.CREATE | FileObserver.DELETE) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                event &= CREATE | DELETE | DELETE_SELF;

                Log.i(Global.TAG ,"Image directory " + path);

                switch (event) {
                    case CREATE:
                    case DELETE:
                        if ( PATTERN_WECHAT_IMAGE_DIRECTORY.matcher(path).matches() )
                            callback.onDirectoryChanged(event ,WECHAT_PATH.getAbsolutePath() + "/" + accountId + "/image2/" + path);
                        break;
                }
            }
        };

        FileObserver exist = accountObservers.get(accountId);
        if ( exist != null )
            exist.stopWatching();
        accountObservers.put(accountId ,observer);

        observer.startWatching();
    }

    private void handleAccountDeleted(String accountId) {
        accountObservers.remove(accountId);
    }

    private void handleDeletedSelf() {
        accountRootObserver.stopWatching();
        accountRootObserver = null;
    }

    private void searchAndPutAccount() {
        for ( String d : WECHAT_PATH.list() ) {
            if ( PATTERN_WECHAT_ACCOUNT_DIRECTORY.matcher(d).matches() )
                handleAccountCreated(d);
        }
    }

    public interface Callback {
        void onDirectoryChanged(int event ,String path);
    }
}
