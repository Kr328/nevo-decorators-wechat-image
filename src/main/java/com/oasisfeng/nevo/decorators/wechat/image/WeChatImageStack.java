package com.oasisfeng.nevo.decorators.wechat.image;

import android.telecom.Call;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WeChatImageStack extends Thread {
    private static final long LOAD_WAIT_MILLIS = 500;

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
        long   postTime;
        Object attach;
    }

    public WeChatImageStack(Callback callback) {
        this.callback = callback;
    }

    public File getImageFileForKey(String key ,int id) {
        synchronized (this) {
            StateRecord record = keyMap.get(key);
            if ( record != null && record.state == State.LOADED && record.id == id )
                return (File) record.attach;
            return null;
        }
    }

    public void postLoadImage(String key ,int id) {
        long currentTime = System.currentTimeMillis();

        synchronized (this) {
            StateRecord record = new StateRecord();

            record.state    = State.WAIT;
            record.key      = key;
            record.id       = id;
            record.postTime = currentTime;
            record.attach   = currentTime;

            keyMap.put(key ,record);
        }
    }

    public void remoteKey(String key) {
        synchronized (this) {
            StateRecord stateRecord = keyMap.remove(key);
            if ( stateRecord != null )
                stateRecord.state = State.REMOVED;
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while ( true ) {
            long minimumWaitTime = -1;
            long currentTime = System.currentTimeMillis();
            ArrayList<StateRecord> stateRecords = new ArrayList<>();

            synchronized (this) {
                for ( Map.Entry<String ,StateRecord> entry : keyMap.entrySet() ) {
                    if ( entry.getValue().state != State.WAIT )
                        continue;

                    long postLoadTime = (long) entry.getValue().postTime;

                    if ( postLoadTime + LOAD_WAIT_MILLIS < currentTime ) {
                        stateRecords.add(entry.getValue());
                        entry.getValue().state = State.LOADING;
                    }
                    else
                        minimumWaitTime = postLoadTime;
                }

                try {
                    if ( stateRecords.isEmpty() ) {
                        if ( minimumWaitTime == -1 )
                            this.wait();
                        else
                            this.wait(minimumWaitTime + LOAD_WAIT_MILLIS - currentTime);
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }

            for ( StateRecord stateRecord : stateRecords ) {
                File file = wechatImageLoader.loadImage(stateRecord.postTime);

                synchronized (this) {
                    if ( stateRecord.state != State.LOADING )
                        continue;

                    stateRecord.attach = file;
                    stateRecord.state  = State.LOADED;
                }
            }
        }
    }

    private HashMap<String ,StateRecord> keyMap = new HashMap<>();
    private WeChatImageLoader wechatImageLoader = new WeChatImageLoader();
    private Callback callback;
}
