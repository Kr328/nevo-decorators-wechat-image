package com.oasisfeng.nevo.decorators.wechat.image;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.oasisfeng.nevo.decorators.wechat.image.Global.TAG;

public class WeChatImageStack extends Thread {
    private static final long LOAD_WAIT_MILLIS = 3000;

    enum State {
        WAIT ,
        LOADING ,
        LOADED ,
        REMOVED
    }

    public interface Callback {
        void onImageLoaded(String key ,int id);
    }

    private class StateRecord {
        State  state;
        String key;
        int    id;
        long   post;
        long   when;
        File   result;
    }

    WeChatImageStack(Callback callback) {
        this.callback = callback;
    }

    File getImageFileForKey(String key ,int id) {
        Log.d(TAG ,"Stack get " + key);

        synchronized (this) {
            StateRecord record = keyMap.get(key);
            if ( record != null && record.state == State.LOADED && record.id == id )
                return record.result;

            return null;
        }
    }

    void postLoadImage(String key ,int id ,long when) {
        if ( !wechatImageLoader.tryInitialize() )
            return;

        long currentTime = System.currentTimeMillis();

        synchronized (this) {
            StateRecord record = new StateRecord();

            record.state     = State.WAIT;
            record.key       = key;
            record.id        = id;
            record.post      = currentTime;
            record.when      = when;

            keyMap.put(key ,record);

            this.notify();
        }

        Log.d(TAG ,"Stack post " + key);
    }

    void removeKey(String key) {
        synchronized (this) {
            StateRecord stateRecord = keyMap.remove(key);
            if ( stateRecord != null )
                stateRecord.state = State.REMOVED;
        }

        Log.d(TAG ,"Stack remove " + key);
    }

    @Override
    public void run() {
        while ( true ) {
            Log.d(TAG ,"Stack loop");

            if ( isInterrupted() )
                return;

            long minimumWaitTime = -1;
            long currentTime = System.currentTimeMillis();
            ArrayList<StateRecord> stateRecords = new ArrayList<>();

            synchronized (this) {
                for ( Map.Entry<String ,StateRecord> entry : keyMap.entrySet() ) {
                    if ( entry.getValue().state != State.WAIT )
                        continue;

                    long postLoadTime = entry.getValue().post;

                    if ( postLoadTime + LOAD_WAIT_MILLIS < currentTime ) {
                        stateRecords.add(entry.getValue());
                        entry.getValue().state = State.LOADING;
                    }
                    else
                        minimumWaitTime = postLoadTime + LOAD_WAIT_MILLIS;
                }

                try {
                    if ( stateRecords.isEmpty() ) {
                        if ( minimumWaitTime == -1 )
                            this.wait();
                        else
                            this.wait(minimumWaitTime - currentTime);
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }

            for ( StateRecord stateRecord : stateRecords ) {
                File file = wechatImageLoader.loadImage(stateRecord.when);

                synchronized (this) {
                    if ( stateRecord.state != State.LOADING )
                        continue;

                    if ( file == null ) {
                        Log.w(TAG ,"Load image failure");
                        keyMap.remove(stateRecord.key);
                        stateRecord.state = State.REMOVED;
                        continue;
                    }

                    stateRecord.result = file;
                    stateRecord.state  = State.LOADED;

                    callback.onImageLoaded(stateRecord.key ,stateRecord.id);
                }
            }
        }
    }

    private HashMap<String ,StateRecord> keyMap = new HashMap<>();
    private WeChatImageLoader wechatImageLoader = new WeChatImageLoader();
    private Callback callback;
}
