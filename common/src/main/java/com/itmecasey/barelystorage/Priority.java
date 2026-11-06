package com.itmecasey.barelystorage;

/** Order in which a network offers items to storage: High first, then Normal, then Low. */
public enum Priority {
    HIGH, NORMAL, LOW;

    public Priority next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Priority byOrdinal(int i) {
        return i < 0 || i >= values().length ? NORMAL : values()[i];
    }

    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
