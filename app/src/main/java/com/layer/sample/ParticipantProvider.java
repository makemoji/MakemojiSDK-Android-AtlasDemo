package com.layer.sample;

import java.util.Map;

/**
 * ParticipantProvider provides classes with Participant data.
 */
public interface ParticipantProvider {
    /**
     * Returns a map of all Participants by their unique ID who match the provided `filter`, or
     * all Participants if `filter` is `null`.  If `result` is provided, it is operated on and
     * returned.  If `result` is `null`, a new Map is created and returned.
     *
     * @param filter The filter to apply to Participants
     * @param result The Map to operate on
     * @return A Map of all matching Participants keyed by ID.
     */
    Map<String, Participant> getMatchingParticipants(String filter, Map<String, Participant> result);

    /**
     * Returns the Participant with the given ID, or `null` if the participant is not yet
     * available.
     *
     * @return The Participant with the given ID, or `null` if not available.
     */
    Participant getParticipant(String userId);
}
