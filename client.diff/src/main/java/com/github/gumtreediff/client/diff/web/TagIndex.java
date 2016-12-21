/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client.diff.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagIndex {

    private Map<Integer, List<String>> startTags;

    private Map<Integer, List<String>> endTags;

    public TagIndex() {
        startTags = new HashMap<Integer, List<String>>();
        endTags = new HashMap<Integer, List<String>>();
    }

    public void addTags(int pos, String startTag, int endPos, String endTag) {
        addStartTag(pos, startTag);
        addEndTag(endPos, endTag);
    }

    public void addStartTag(int pos, String tag) {
        if (!startTags.containsKey(pos)) startTags.put(pos, new ArrayList<String>());
        startTags.get(pos).add(tag);
    }

    public void addEndTag(int pos, String tag) {
        if (!endTags.containsKey(pos)) endTags.put(pos, new ArrayList<String>());
        endTags.get(pos).add(tag);
    }

    public String getEndTags(int pos) {
        if (!endTags.containsKey(pos)) return "";
        StringBuffer b = new StringBuffer();
        for (String s: endTags.get(pos)) b.append(s);
        return b.toString();
    }

    public String getStartTags(int pos) {
        if (!startTags.containsKey(pos))
            return "";
        StringBuffer b = new StringBuffer();
        for (String s: startTags.get(pos))
            b.append(s);
        return b.toString();
    }

}
