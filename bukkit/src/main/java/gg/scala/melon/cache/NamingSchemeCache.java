package gg.scala.melon.cache;

import gg.scala.melon.commons.cache.ClassCache;
import gg.scala.melon.commons.scheme.NamingScheme;
import gg.scala.melon.commons.scheme.impl.CompletelyRandomNamingScheme;
import gg.scala.melon.commons.scheme.impl.PhraseBasedNamingScheme;

import java.util.HashMap;
import java.util.Map;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

public class NamingSchemeCache extends ClassCache<NamingScheme> {

    private static final NamingSchemeCache INSTANCE;

    static {
        INSTANCE = new NamingSchemeCache();
    }

    private final Map<Class<? extends NamingScheme>, NamingScheme> classNamingSchemeHashMap = new HashMap<>();

    {
        this.getCache().put(CompletelyRandomNamingScheme.class, new CompletelyRandomNamingScheme());
        this.getCache().put(PhraseBasedNamingScheme.class, new PhraseBasedNamingScheme());
    }

    @Override
    public Map<Class<? extends NamingScheme>, NamingScheme> getCache() {
        return this.classNamingSchemeHashMap;
    }

    public static NamingSchemeCache get() {
        return NamingSchemeCache.INSTANCE;
    }
}
