package com.envarcade.brennon.api.chat;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Defines a chat filter rule that can block, censor, or flag messages.
 */
public interface ChatFilter {

    /** Unique filter ID. */
    String getId();

    /** The regex patterns this filter matches. */
    List<Pattern> getPatterns();

    /** What action to take when a message matches. */
    FilterAction getAction();

    /** Replacement text when action is CENSOR. */
    String getReplacement();

    /** Whether this filter is currently enabled. */
    boolean isEnabled();

    enum FilterAction {
        /** Block the message entirely. */
        BLOCK,
        /** Replace matched text with replacement. */
        CENSOR,
        /** Allow the message but flag it for staff review. */
        FLAG,
        /** Allow the message but log it. */
        LOG
    }
}
