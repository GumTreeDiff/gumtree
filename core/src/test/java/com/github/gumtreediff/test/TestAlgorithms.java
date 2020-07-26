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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.utils.HungarianAlgorithm;
import com.github.gumtreediff.utils.StringAlgorithms;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.List;

public class TestAlgorithms {

    @Test
    public void testLcss() {
    	List<int[]> expected = new ArrayList<int[]>();
    	expected.add(new int[] {0, 0});
    	expected.add(new int[] {3, 2});
    	expected.add(new int[] {5, 4});
    	
    	// Exemple coming from:
        // http://www.geeksforgeeks.org/dynamic-programming-set-4-longest-common-subsequence/
        List<int[]> indexes = StringAlgorithms.lcss("ABCDGH", "AEDFHR");
        
        assertArrayEquals(expected.toArray(), indexes.toArray());
    }

    @Test
    public void testLcs() {
        String lcs = StringAlgorithms.lcs("FUTUR", "CHUTE");
        String expected = "UT";
        assertEquals(expected, lcs);        
    }

    @Test
    public void testHungarianAlgorithm() {
        // Exemple coming from https://en.wikipedia.org/wiki/Hungarian_algorithm
        double[][] costMatrix = new double[3][];
        costMatrix[0] = new double[] {2F, 3F, 3F};
        costMatrix[1] = new double[] {3F, 2F, 3F};
        costMatrix[2] = new double[] {3F, 3F, 2F};
        HungarianAlgorithm a = new HungarianAlgorithm(costMatrix);
        int[] result = a.execute();
        int[] expected = {0, 1, 2};
        Assert.assertArrayEquals(expected, result);        
    }

}
