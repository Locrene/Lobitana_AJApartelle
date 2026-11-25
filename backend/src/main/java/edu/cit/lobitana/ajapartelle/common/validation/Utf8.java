package edu.cit.lobitana.ajapartelle.common.validation;

import java.nio.charset.StandardCharsets;

public final class Utf8 {
    private Utf8() {}

    /** Length as bcrypt sees it: UTF-8 bytes, not characters. */
    public static int byteLength(CharSequence value) {
        return value.toString().getBytes(StandardCharsets.UTF_8).length;
    }
}
