package com.solexgames.queue.commons.scheme.impl;

import com.solexgames.queue.commons.scheme.NamingScheme;

import java.util.Arrays;
import java.util.List;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

public class PhraseBasedNamingScheme implements NamingScheme {

    private final List<String> phrases = Arrays.asList(
            "hors", "pig"
    );

    @Override
    public String getId() {
        return "phrase";
    }

    @Override
    public String generate() {
        return "";
    }
}
