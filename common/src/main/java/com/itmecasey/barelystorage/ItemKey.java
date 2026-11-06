package com.itmecasey.barelystorage;

/** What makes two stacks "the same item" in a network: the registry id plus an opaque string of the stack's data components. */
public record ItemKey(String id, String data) implements Comparable<ItemKey> {
    public ItemKey {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("empty item id");
        }
        data = data == null ? "" : data;
    }

    public static ItemKey of(String id) {
        return new ItemKey(id, "");
    }

    @Override
    public int compareTo(ItemKey o) {
        int c = id.compareTo(o.id);
        return c != 0 ? c : data.compareTo(o.data);
    }

    /** One line text form, stable across versions, used by saved data. */
    public String encode() {
        return data.isEmpty() ? id : id + "\u001e" + data;
    }

    public static ItemKey decode(String text) {
        int i = text.indexOf('\u001e');
        return i < 0 ? new ItemKey(text, "") : new ItemKey(text.substring(0, i), text.substring(i + 1));
    }
}
