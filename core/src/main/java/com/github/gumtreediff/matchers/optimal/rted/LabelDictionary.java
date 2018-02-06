//    Copyright (C) 2012  Mateusz Pawlik and Nikolaus Augsten
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as
//    published by the Free Software Foundation, either version 3 of the
//    License, or (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU Affero General Public License for more details.
//
//    You should have received a copy of the GNU Affero General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.github.gumtreediff.matchers.optimal.rted;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * This provides a way of using small int values to represent String labels,
 * as opposed to storing the labels directly.
 *
 * @author Denilson Barbosa, Nikolaus Augsten  from approxlib, available at http://www.inf.unibz.it/~augsten/src/
 */
public class LabelDictionary {
    public static final int KEY_DUMMY_LABEL = -1;
    private int count;
    private Map<String, Integer> strInt;
    private Map<Integer, String> intStr;
    private boolean newLabelsAllowed = true;

    /**
     * Creates a new blank dictionary.
     */
    public LabelDictionary() {
        count = 0;
        strInt = new HashMap<>();
        intStr = new HashMap<>();
    }


    /**
     * Adds a new label to the dictionary if it has not been added yet.
     * Returns the ID of the new label in the dictionary.
     *
     * @param label add this label to the dictionary if it does not exist yet
     * @return ID of label in the dictionary
     */
    public int store(String label) {
        if (strInt.containsKey(label)) {
            return (strInt.get(label).intValue());
        } else if (!newLabelsAllowed) {
            return KEY_DUMMY_LABEL;
        } else { // store label
            Integer intKey = count++;
            strInt.put(label, intKey);
            intStr.put(intKey, label);
            return intKey.intValue();
        }
    }

    /**
     * Returns the label with a given ID in the dictionary.
     *
     * @param labelID
     * @return the label with the specified labelID, or null if this dictionary contains no label for labelID
     */
    public String read(int labelID) {
        return intStr.get(Integer.valueOf(labelID));
    }

    /**
     * @return true iff new labels can be stored into this label dictinoary
     */
    public boolean isNewLabelsAllowed() {
        return newLabelsAllowed;
    }

    /**
     * @param newLabelsAllowed the newLabelsAllowed to set
     */
    public void setNewLabelsAllowed(boolean newLabelsAllowed) {
        this.newLabelsAllowed = newLabelsAllowed;
    }

}

