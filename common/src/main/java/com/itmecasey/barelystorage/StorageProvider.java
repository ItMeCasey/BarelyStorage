package com.itmecasey.barelystorage;

import java.util.Map;

/** Anything a network can store items in or take items from: a rack's cartridge or an inventory behind a Storage Bus. */
public interface StorageProvider {
    Priority priority();

    /** Adds every item type held to {@code out}. */
    void collect(Map<ItemKey, Long> out);

    long count(ItemKey key);

    /** @return how many of {@code amount} were taken in */
    long insert(ItemKey key, long amount, boolean simulate);

    /** @return how many were handed out */
    long extract(ItemKey key, long amount, boolean simulate);
}
