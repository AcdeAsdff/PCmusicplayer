package com.linearity.pcmusicplayer;

import java.io.File;

public record SongBean(File songFile,int index) {


    @Override
    public String toString() {
        return index + ": " + songFile.getName();
    }
}
