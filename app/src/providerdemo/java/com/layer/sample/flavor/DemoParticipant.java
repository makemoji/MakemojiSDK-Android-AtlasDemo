package com.layer.sample.flavor;

import android.support.annotation.NonNull;

import com.layer.sample.Participant;

public class DemoParticipant implements Participant {
    private String mId;
    private String mName;

    @Override
    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    @Override
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public int compareTo(@NonNull Participant another) {
        return getName().toLowerCase().compareTo(another.getName().toUpperCase());
    }
}
