package edu.cit.lobitana.ajapartelle.dto;

import java.util.Map;

/** SRS Appendix E error format: {"code", "message"} (+ optional field errors). */
public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    public ApiError(String code, String message) { this(code, message, null); }
}
