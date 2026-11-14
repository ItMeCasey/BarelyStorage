package com.itmecasey.barelystorage.client;

import java.util.function.Consumer;

/**
 * The player's screen choices (theme, terminal height, sort, order and view). They live in the loader's client config
 * ({@code config/barelystorage-client.toml}): the loader pushes the loaded values in through {@link #apply} and gets the new
 * ones back through {@link #sink} when a button changes one.
 */
public final class ClientSettings {
    public static int theme;
    public static int height;
    public static int sort;
    public static int order;
    public static int view;
    /** Set by the loader: stores {theme, height, sort, order, view} in its config file. */
    public static Consumer<int[]> sink = values -> {
    };

    private ClientSettings() {
    }

    /** Takes the values of the loaded (or reloaded) client config. */
    public static void apply(int theme, int height, int sort, int order, int view) {
        ClientSettings.theme = theme;
        ClientSettings.height = height;
        ClientSettings.sort = sort;
        ClientSettings.order = order;
        ClientSettings.view = view;
        Ui.theme(theme == 1);
    }

    /** Called when a screen opens; the values are already in place, this only makes the theme current. */
    public static void load() {
        Ui.theme(theme == 1);
    }

    public static void save() {
        sink.accept(new int[] {theme, height, sort, order, view});
    }
}
