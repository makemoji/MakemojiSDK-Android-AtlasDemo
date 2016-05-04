package com.layer.sample.flavor;

import android.support.annotation.NonNull;

import com.layer.sample.Participant;

public class RailsParticipant implements Participant {
    private String mId;
    private String mFirstName;
    private String mLastName;
    private String mEmail;

    @Override
    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    @Override
    public String getName() {
        return (getFirstName() + " " + getLastName()).trim();
    }

    @Override
    public int compareTo(@NonNull Participant another) {
        int first = getFirstName().toLowerCase().compareTo(((RailsParticipant) another).getFirstName().toLowerCase());
        if (first != 0) return first;
        return getLastName().toLowerCase().compareTo(((RailsParticipant) another).getLastName().toLowerCase());
    }
}
