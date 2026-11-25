package edu.cit.lobitana.ajapartelle.user.dto;

import edu.cit.lobitana.ajapartelle.common.security.StrictBCryptPasswordEncoder;

/** Field rules shared by registration, staff creation, profile and password forms (FR-071 to FR-075). */
public final class AccountRules {
    public static final int NAME_MIN = 2;
    public static final int NAME_MAX = 100;
    public static final String NAME_SIZE = "Full name must be 2 to 100 characters";

    public static final String MOBILE_REGEX = "^09\\d{9}$";
    public static final String MOBILE_FORMAT = "Mobile number must be in the format 09XXXXXXXXX (11 digits)";

    public static final int PASSWORD_MIN = 8;
    public static final String PASSWORD_TOO_SHORT = "Password must be at least 8 characters";
    public static final int PASSWORD_MAX_BYTES = StrictBCryptPasswordEncoder.MAX_BYTES;
    public static final String PASSWORD_TOO_LONG =
            "Password is too long. The limit is 72 bytes: 72 plain letters or numbers, fewer with accented letters or emoji";
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String PASSWORD_FORMAT = "Password must contain at least one letter and one number";

    private AccountRules() {}
}
