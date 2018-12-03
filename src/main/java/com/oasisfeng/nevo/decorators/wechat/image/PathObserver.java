package com.oasisfeng.nevo.decorators.wechat.image;

import android.os.FileObserver;
import android.telecom.Call;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Vector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PathObserver {
    public interface Callback {
        void onEvent(int event ,String path);
    }

    public PathObserver(String path , int mask , Callback callback) {
        this.path     = new File(path).getAbsolutePath();
        this.mask     = mask;
        this.callback = callback;
    }

    public synchronized void startWatching() {
        if ( currentObserver != null )
            currentObserver.stopWatching();

        String available = findObservablePath();
        if ( available.isEmpty() ) {
            callback.onEvent(FileObserver.DELETE_SELF ,path);
            return;
        }

        if ( available.equals(path) ) {
            (currentObserver = new FileObserver(available ,mask) {
                @Override
                public void onEvent(int event, @Nullable String path) {
                    callback.onEvent(event ,path);
                    if ( event == FileObserver.DELETE_SELF || event == FileObserver.MOVE_SELF )
                        PathObserver.this.startWatching();
                }
            }).startWatching();
            return;
        }

        (currentObserver = new FileObserver(available ,FileObserver.CREATE) {
            @Override
            public void onEvent(int event, @Nullable String p) {
                Log.d(TAG ,"Subdirectory created " + p);
                if ( path.startsWith(available + "/" + p) )
                    PathObserver.this.startWatching();
            }
        }).startWatching();
    }

    private String findObservablePath() {
        String currentPath = path;

        while ( !currentPath.isEmpty() ) {
            if ( new File(currentPath).exists() )
                break;
            currentPath = currentPath.substring(0 ,currentPath.lastIndexOf('/'));
        }

        return new File(currentPath).getAbsolutePath();
    }

    private static final String TAG = PathObserver.class.getSimpleName();

    private String   path;
    private int      mask;
    private Callback callback;

    private FileObserver currentObserver;
}
