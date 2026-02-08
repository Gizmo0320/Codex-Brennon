package com.envarcade.brennon.api.stats;

/**
 * Built-in stat IDs tracked by Brennon.
 * Plugins can define additional custom stats.
 */
public final class StatTypes {

    private StatTypes() {}

    // Time
    public static final String PLAYTIME = "playtime";
    public static final String SESSIONS = "sessions";

    // PvP
    public static final String KILLS = "kills";
    public static final String DEATHS = "deaths";
    public static final String KDR = "kdr";
    public static final String KILL_STREAK = "kill_streak";
    public static final String HIGHEST_KILL_STREAK = "highest_kill_streak";

    // Economy
    public static final String MONEY_EARNED = "money_earned";
    public static final String MONEY_SPENT = "money_spent";

    // Blocks
    public static final String BLOCKS_PLACED = "blocks_placed";
    public static final String BLOCKS_BROKEN = "blocks_broken";

    // Chat
    public static final String MESSAGES_SENT = "messages_sent";

    // Punishments
    public static final String TIMES_BANNED = "times_banned";
    public static final String TIMES_MUTED = "times_muted";
    public static final String TIMES_KICKED = "times_kicked";
    public static final String TIMES_WARNED = "times_warned";
    public static final String TIMES_IP_BANNED = "times_ip_banned";

    // Reports
    public static final String REPORTS_FILED = "reports_filed";
    public static final String REPORTS_RECEIVED = "reports_received";

    // Tickets
    public static final String TICKETS_CREATED = "tickets_created";
    public static final String TICKETS_RESOLVED = "tickets_resolved";
}
