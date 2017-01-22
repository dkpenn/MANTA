package com.mymanet.manta;

/**
 * Created by dk on 12/4/16.
 */

public class MantaFile {
    private long id;
    private String filename;

    public MantaFile()
    {

    }

    public MantaFile(String filename)
    {
        this.filename = filename;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String toString() {
        return filename;
    }
}
