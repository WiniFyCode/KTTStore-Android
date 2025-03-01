package com.baconga.kttstore.Models;

public class MTarget {
    public String targetID;
    public String name;

    public MTarget(String targetID, String name) {
        this.targetID = targetID;
        this.name = name;
    }

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
