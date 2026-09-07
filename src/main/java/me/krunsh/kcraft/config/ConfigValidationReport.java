package me.krunsh.kcraft.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Rapport de validation lisible et réutilisable au boot/reload. */
public final class ConfigValidationReport {

    private final List<String> errors = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();

    public void error(String message) {
        if (message != null && !message.trim().isEmpty()) errors.add(message.trim());
    }

    public void warning(String message) {
        if (message != null && !message.trim().isEmpty()) warnings.add(message.trim());
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public int errorCount() { return errors.size(); }
    public int warningCount() { return warnings.size(); }

    public void merge(ConfigValidationReport other) {
        if (other == null) return;
        errors.addAll(other.errors);
        warnings.addAll(other.warnings);
    }
}
