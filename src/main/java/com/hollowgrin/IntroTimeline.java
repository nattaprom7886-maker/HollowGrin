package com.hollowgrin;

/**
 * Timing of the world-entry intro (20 ticks = 1 second). Shared by server (protection window) and client (screen).
 * The numbers match tools/make_intro.py output. If you replace intro_voice.ogg / intro_laugh.ogg with your own
 * recordings, adjust VOICE_START, the LINES and the EYES_OUT / LAUGH numbers to fit.
 */
public final class IntroTimeline {
    /** Server waits this long after joining before starting (lets the world finish loading). */
    public static final int START_DELAY_TICKS = 40;
    /** A world counts as "brand new" if the overworld clock is below this when the player first joins (2 minutes). */
    public static final long NEW_WORLD_TICKS = 2400L;

    public static final int EYES_IN_START = 40;
    public static final int EYES_IN_END = 100;
    public static final int VOICE_START = 110;
    public static final int SQUINT_START = 404;
    public static final int SQUINT_END = 437;
    public static final int EYES_OUT_START = 877;
    public static final int EYES_OUT_END = 891;
    public static final int LAUGH_START = 887;
    /** Black fades away over the last FADE_OUT_TICKS (the laugh tail keeps playing into the game). */
    public static final int FADE_OUT_TICKS = 30;
    public static final int TOTAL_TICKS = 1017;

    public record Line(String key, int start, int end) {}

    public static final Line[] LINES = {
            new Line("intro.hollowgrin.l1", 110, 135),
            new Line("intro.hollowgrin.l2", 146, 202),
            new Line("intro.hollowgrin.l3", 219, 249),
            new Line("intro.hollowgrin.l4", 261, 346),
            new Line("intro.hollowgrin.l5", 360, 392),
            new Line("intro.hollowgrin.l6", 404, 425),
            new Line("intro.hollowgrin.l7", 444, 554),
            new Line("intro.hollowgrin.l8", 573, 653),
            new Line("intro.hollowgrin.l9", 668, 726),
            new Line("intro.hollowgrin.l10", 741, 770),
            new Line("intro.hollowgrin.l11", 789, 863)
    };

    private IntroTimeline() {}
}
