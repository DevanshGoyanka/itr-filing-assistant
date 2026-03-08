package com.itr.model;

public enum EmployerCategory {
    CENTRAL_GOVT("CG"),
    STATE_GOVT("SG"),
    PSU("PSU"),
    PENSIONER("PEN"),
    OTHER("OTH"),
    NOT_APPLICABLE("NA");

    private final String code;

    EmployerCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static EmployerCategory fromCode(String code) {
        if (code == null) return OTHER;
        for (EmployerCategory ec : values()) {
            if (ec.code.equalsIgnoreCase(code)) {
                return ec;
            }
        }
        return OTHER;
    }

    public boolean isCentralOrStateGovt() {
        return this == CENTRAL_GOVT || this == STATE_GOVT || this == PSU;
    }

    public boolean isPensioner() {
        return this == PENSIONER;
    }
}
