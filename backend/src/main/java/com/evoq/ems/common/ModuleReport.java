package com.evoq.ems.common;

import java.util.List;
import java.util.Map;

/** Transport only. Each module owns its scope, facts and calculations. */
public record ModuleReport(String scope, Map<String, Number> summary, List<Table> tables) {
    public record Table(String title, List<String> columns, List<List<String>> rows) { }
    public static String text(Object value) { return value == null ? "" : value.toString(); }
}
