package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;

public class FixedMatcher extends Matcher {

    public FixedMatcher(MappingStore m) {
        super(null, null, m);
    }

    @Override
    public void match() {}
}
