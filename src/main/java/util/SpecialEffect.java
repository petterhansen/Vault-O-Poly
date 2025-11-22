package util;

import java.io.Serializable;

public enum SpecialEffect implements Serializable {
    GAIN_CAPS,
    LOSE_CAPS,
    GO_TO_FIELD,
    DRAW_EVENT,
    NOTHING;

    private static final long serialVersionUID = 1L;
}
