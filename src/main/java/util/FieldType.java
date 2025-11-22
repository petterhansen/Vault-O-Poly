package util;

import java.io.Serializable;

public enum FieldType implements Serializable {
    START,
    PROPERTY,
    RESOURCE,
    SPECIAL;

    private static final long serialVersionUID = 1L;
}