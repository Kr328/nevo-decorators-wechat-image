package com.oasisfeng.nevo.decorators.wechat.image;

import java.io.File;
import java.util.ArrayList;

public class Files {
    public static ArrayList<File> walkFiles(File base) {
        ArrayList<File> result = new ArrayList<>();
        append(result ,base);
        return result;
    }

    private static void append(ArrayList<File> output ,File base) {
        if ( !base.isDirectory() )
            return;

        for ( File f : base.listFiles() )
            append(output ,f);
    }
}
