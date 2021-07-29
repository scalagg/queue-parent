package com.solexgames.queue.commons.scheme.impl;

import com.solexgames.queue.commons.scheme.NamingScheme;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

public class PhraseBasedNamingScheme implements NamingScheme {

    @Override
    public String getId() {
        return "phrase";
    }

    @Override
    public String generate() {
        return "";
    }
}
