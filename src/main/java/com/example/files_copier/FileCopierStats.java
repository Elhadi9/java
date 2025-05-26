package com.example.files_copier;

import com.fasterxml.jackson.annotation.JsonFormat;

public class FileCopierStats {
    private int success;
    private int fail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String copyTime;

    // Constructors
    public FileCopierStats() {
    }

    public FileCopierStats(int success, int fail, String readtime) {
        this.success = success;
        this.fail = fail;
        this.copyTime = readtime;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFail() {
        return fail;
    }

    public void setFail(int fail) {
        this.fail = fail;
    }

    public String getCopyTime() {
        return copyTime;
    }

    public void setCopyTime(String copyTime) {
        this.copyTime = copyTime;
    }
}
