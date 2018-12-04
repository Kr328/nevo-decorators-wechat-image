package com.oasisfeng.nevo.decorators.wechat.image;

import java.io.File;
import java.util.ArrayList;

import java9.util.stream.Stream;

class FilesUtils {
    static Stream<File> listEntry(File base) {
        Stream.Builder<File> builder = Stream.builder();

        listFiles(builder ,base);

        return builder.build();
    }

    private static void listFiles(Stream.Builder<File> output , File base) {
        output.accept(base);

        if ( !base.isDirectory() )
            return;

        for ( File f : base.listFiles() )
            listFiles(output ,f);
    }
}
