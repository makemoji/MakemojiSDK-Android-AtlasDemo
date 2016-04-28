package com.layer.sample;

import android.net.Uri;

public interface Participant extends Comparable<Participant> {

    String getId();
    String getName();
    Uri getAvatarUrl();
}
