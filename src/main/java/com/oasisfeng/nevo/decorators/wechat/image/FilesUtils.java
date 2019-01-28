package com.oasisfeng.nevo.decorators.wechat.image;

import java.io.File;
import java.util.ArrayList;

import java9.util.stream.Stream;

class FilesUtils {
    static Stream<File> listAllFrom(File... dirs) {
        Stream.Builder<File> builder = Stream.builder();

        for ( File base : dirs )
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
