package com.solexgames.queue.commons.scheme.impl;

import com.solexgames.queue.commons.scheme.NamingScheme;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

public class CompletelyRandomNamingScheme implements NamingScheme {

    @Override
    public String getId() {
        return "random";
    }

    @Override
    public String generate() {
        return "";
    }
}
