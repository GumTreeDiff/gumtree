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

import com.github.gumtreediff.tree.Tree;

import java.util.*;


/**
 * Stores all needed information about a single tree in several indeces. 
 *
 * @author Mateusz Pawlik
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class InfoTree {
    private Tree inputTree;

    private static final byte LEFT = 0;
    private static final byte RIGHT = 1;
    private static final byte HEAVY = 2;
    private static final byte BOTH = 3;
    //private static final byte REVLEFT = 4;
    //private static final byte REVRIGHT = 5;
    //private static final byte REVHEAVY = 6;

    // constants for indeces numbers
    public static final byte POST2_SIZE = 0;
    public static final byte POST2_KR_SUM = 1;
    public static final byte POST2_REV_KR_SUM = 2;
    public static final byte POST2_DESC_SUM = 3; // number of subforests in full decomposition
    public static final byte POST2_PRE = 4;
    public static final byte POST2_PARENT = 5;
    public static final byte POST2_LABEL = 6;
    public static final byte KR = 7; // key root nodes (size of this array = leaf count)
    public static final byte POST2_LLD = 8; // left-most leaf descendants
    public static final byte POST2_MIN_KR = 9; // minimum key root nodes index in KR array
    public static final byte RKR = 10; // reversed key root nodes
    public static final byte RPOST2_RLD = 11; // reversed postorer 2 right-most leaf descendants
    public static final byte RPOST2_MIN_RKR = 12;
    public static final byte RPOST2_POST = 13; // reversed postorder -> postorder
    public static final byte POST2_STRATEGY = 14; // strategy for Demaine (is there sth on the left/right of the heavy node)
    public static final byte PRE2_POST = 15;

    public int[][] info; // an array with all the indeces
    
    private LabelDictionary ld; // dictionary with labels - common for two input trees

    public boolean[][] nodeType; // store the type of a node: for every node stores three boolean values (L, R, H)
    
    // paths and rel subtrees are inside 2D arrays to be able to get them by paths/relsubtrees[L/R/H][node]
    private int[][] paths;
    private int[][][] relSubtrees;

    // temporal variables
    private int sizeTmp = 0; // temporal value of size of a subtree
    private int descSizesTmp = 0; // temporal value of sum of descendat sizes
    private int krSizesSumTmp = 0; // temporal value of sum of key roots sizes
    private int revkrSizesSumTmp = 0; // temporal value of sum of reversed hey roots sizes
    private int preorderTmp = 0; // temporal value of preorder
    
    // remembers what is the current node's postorder (current in the TED recursion)
    private int currentNode = -1;
    
    // remembers if the trees order was switched during the recursion (in comparison with the order of input trees)
    private boolean switched = false;
    
    // as the names say
    private int leafCount = 0;
    private int treeSize = 0;

    
    public static void main(String[] args) {

    }

    /**
     * Creates an InfoTree object, gathers all information about aInputTree and stores in indexes.
     * aInputTree is not needed any more.
     * Remember to pass the same LabelDictionary object to both trees which are compared.
     * 
     * @param aInputTree an LblTree object
     * @param aLd  a LabelDictionary object
     */
    public InfoTree(Tree aInputTree, LabelDictionary aLd) {
        this.inputTree = aInputTree;
        treeSize = inputTree.getMetrics().size;
        this.info = new int[16][treeSize];
        Arrays.fill(info[POST2_PARENT], -1);
        Arrays.fill(info[POST2_MIN_KR], -1);
        Arrays.fill(info[RPOST2_MIN_RKR], -1);
        Arrays.fill(info[POST2_STRATEGY], -1);
        this.paths = new int[3][treeSize]; Arrays.fill(paths[LEFT], -1); Arrays.fill(paths[RIGHT], -1); Arrays.fill(paths[HEAVY], -1);
        this.relSubtrees = new int[3][treeSize][];
        this.nodeType = new boolean[3][treeSize];
        this.ld = aLd;
        this.currentNode = treeSize - 1;
        gatherInfo(inputTree, -1);
        postTraversalProcessing();
    }

    /**
     * Returns the size of the tree.
     * 
     * @return
     */
    public int getSize() {
        return treeSize;
    }
    
    public boolean ifNodeOfType(int postorder, int type) {
        return nodeType[type][postorder];
    }
    
    public boolean[] getNodeTypeArray(int type) {
        return nodeType[type];
    }
    
        
    /**
     * For given infoCode and postorder of a node returns requested information of that node.
     * 
     * @param infoCode
     * @param nodesPostorder postorder of a node
     * @return a value of requested information
     */
    public int getInfo(int infoCode, int nodesPostorder) {
        // return info under infoCode and nodesPostorder
        return info[infoCode][nodesPostorder];
    }
    
    /**
     * For given infoCode returns an info array (index array)
     * 
     * @param infoCode
     * @return array with requested index
     */
    public int[] getInfoArray(int infoCode) {
        return info[infoCode];
    }
    
    /**
     * Returns relevant subtrees for given node. Assuming that child v of given node belongs to given path, 
     * all children of given node are returned but node v.
     * 
     * @param pathType
     * @param nodePostorder postorder of a node
     * @return  an array with relevant subtrees of a given node
     */
    public int[] getNodeRelSubtrees(int pathType, int nodePostorder) {
        return relSubtrees[pathType][nodePostorder];
    }
    
    /**
     * Returns an array representation of a given path's type.
     * 
     * @param pathType
     * @return an array with a requested path
     */
    public int[] getPath(int pathType) {
        return paths[pathType];
    }
    
    /**
     * Returns the postorder of current root node.
     * 
     * @return 
     */
    public int getCurrentNode() {
        return currentNode;
    }
    
    /**
     * Sets postorder of the current node in the recursion.
     * 
     * @param postorder 
     */
    public void setCurrentNode(int postorder) {
        currentNode = postorder;
    }
    
    /**
     * Gathers information of a given tree in corresponding arrays.
     * 
     * At this point the given tree is traversed once, but there is a loop over current nodes children
     * to assign them their parents.
     * 
     * @param aT
     * @param postorder
     * @return 
     */
	private int gatherInfo(Tree aT, int postorder) {
        int currentSize = 0;
        int childrenCount = 0;
        int descSizes = 0;
        int krSizesSum = 0;
        int revkrSizesSum = 0;
        int preorder = preorderTmp;
        
        int heavyChild = -1;
        int leftChild = -1;
        int rightChild = -1;
        int weight = -1;
        int maxWeight = -1;
        int currentPostorder = -1;
        int oldHeavyChild = -1;

        ArrayList heavyRelSubtreesTmp = new ArrayList();
		ArrayList leftRelSubtreesTmp = new ArrayList();
        ArrayList rightRelSubtreesTmp = new ArrayList();
        
        ArrayList<Integer> childrenPostorders = new ArrayList<Integer>();

        preorderTmp++;

        // enumerate over children of current node
        for (Enumeration<?> e = Collections.enumeration(aT.getChildren()); e.hasMoreElements();) {
            childrenCount++;

            postorder = gatherInfo((Tree) e.nextElement(), postorder);

            childrenPostorders.add(postorder);
            
            currentPostorder = postorder;

            // heavy path
            weight = sizeTmp + 1;
            if (weight >= maxWeight) {
                maxWeight = weight;
                oldHeavyChild = heavyChild;
                heavyChild = currentPostorder;
            } else heavyRelSubtreesTmp.add(currentPostorder);
            
            if (oldHeavyChild != -1) {
                heavyRelSubtreesTmp.add(oldHeavyChild);
                oldHeavyChild = -1;
            }

            // left path
            if (childrenCount == 1) leftChild = currentPostorder;
            else leftRelSubtreesTmp.add(currentPostorder);

            // right path
            rightChild = currentPostorder;
            if (e.hasMoreElements()) rightRelSubtreesTmp.add(currentPostorder);

            // subtree size
            currentSize += 1 + sizeTmp;

            descSizes += descSizesTmp;

            if (childrenCount > 1) krSizesSum += krSizesSumTmp + sizeTmp + 1;
            else {
                krSizesSum += krSizesSumTmp;
                nodeType[LEFT][currentPostorder] = true;
            }

            if (e.hasMoreElements()) revkrSizesSum += revkrSizesSumTmp + sizeTmp + 1;
            else {
                revkrSizesSum += revkrSizesSumTmp;
                nodeType[RIGHT][currentPostorder] = true;
            }
        }

        postorder++;

        int currentDescSizes = descSizes + currentSize + 1;
        info[POST2_DESC_SUM][postorder] = (currentSize + 1) * (currentSize + 1 + 3) / 2 - currentDescSizes;
        info[POST2_KR_SUM][postorder] = krSizesSum + currentSize + 1;
        info[POST2_REV_KR_SUM][postorder] = revkrSizesSum + currentSize + 1;

        // POST2_LABEL
        //labels[rootNumber] = ld.store(aT.getLabel());
        info[POST2_LABEL][postorder] = ld.store(aT.getLabel());
        
        // POST2_PARENT
        for (Integer i : childrenPostorders) info[POST2_PARENT][i] = postorder;
     
        // POST2_SIZE
        info[POST2_SIZE][postorder] = currentSize + 1;
        if (currentSize == 0) leafCount++;
        
        // POST2_PRE
        info[POST2_PRE][postorder] = preorder;
        
        // PRE2_POST
        info[PRE2_POST][preorder] = postorder;
        
        // RPOST2_POST
        info[RPOST2_POST][treeSize - 1 - preorder] = postorder;

        // heavy path
        if (heavyChild != -1) {
            paths[HEAVY][postorder] = heavyChild;
            nodeType[HEAVY][heavyChild] = true;

            if (leftChild < heavyChild && heavyChild < rightChild) {
                info[POST2_STRATEGY][postorder] = BOTH;
            } else if (heavyChild == leftChild) {
                info[POST2_STRATEGY][postorder] = RIGHT;
            } else if (heavyChild == rightChild) {
                info[POST2_STRATEGY][postorder] = LEFT;
            }
        } else {
            info[POST2_STRATEGY][postorder] = RIGHT;
        }
        
        
        
        // left path
        if (leftChild != -1) {
            paths[LEFT][postorder] = leftChild;
        }
        // right path
        if (rightChild != -1) {
            paths[RIGHT][postorder] = rightChild;
        }

        // heavy/left/right relevant subtrees
        relSubtrees[HEAVY][postorder] = toIntArray(heavyRelSubtreesTmp);
        relSubtrees[RIGHT][postorder] = toIntArray(rightRelSubtreesTmp);
        relSubtrees[LEFT][postorder] = toIntArray(leftRelSubtreesTmp);

        descSizesTmp = currentDescSizes;
        sizeTmp = currentSize;
        krSizesSumTmp = krSizesSum;
        revkrSizesSumTmp = revkrSizesSum;

        return postorder;
    }
    
    /**
     * Gathers information, that couldn't be collected while tree traversal.
     */
    private void postTraversalProcessing() {
        int nc1 = treeSize;
        info[KR] = new int[leafCount];
        info[RKR] = new int[leafCount];

        int nc = nc1;
        int lc = leafCount;
        int i = 0;
        
        // compute left-most leaf descendants
        // go along the left-most path, remember each node and assign to it the path's leaf
        // compute right-most leaf descendants (in reversed postorder)
        for (i = 0; i < treeSize; i++) {
            if (paths[LEFT][i] == -1) {
                info[POST2_LLD][i] = i;
            } else {
                info[POST2_LLD][i] = info[POST2_LLD][paths[LEFT][i]];
            }
            if (paths[RIGHT][i] == -1) {
                info[RPOST2_RLD][treeSize - 1 - info[POST2_PRE][i]] = (treeSize - 1 - info[POST2_PRE][i]);
            } else {
                info[RPOST2_RLD][treeSize - 1 - info[POST2_PRE][i]] = info[RPOST2_RLD][treeSize - 1 - info[POST2_PRE][paths[RIGHT][i]]];
            }
        }
                
        // compute key root nodes
        // compute reversed key root nodes (in revrsed postorder)
        boolean[] visited = new boolean[nc];
        boolean[] visitedR = new boolean[nc];
        Arrays.fill(visited, false);
        int k = lc - 1;
        int kR = lc - 1;
        for (i = nc - 1; i >= 0; i--) {
            if (!visited[info[POST2_LLD][i]]) {
                info[KR][k] = i;
                visited[info[POST2_LLD][i]] = true;
                k--;
            }
            if (!visitedR[info[RPOST2_RLD][i]]) {
                info[RKR][kR] = i;
                visitedR[info[RPOST2_RLD][i]] = true;
                kR--;
            }
        }
        
        // compute minimal key roots for every subtree
        // compute minimal reversed  key roots for every subtree (in reversed postorder)
        int parent = -1;
        int parentR = -1;
        for (i = 0; i < leafCount; i++) {
            parent = info[KR][i];
            while (parent > -1 && info[POST2_MIN_KR][parent] == -1) {
                info[POST2_MIN_KR][parent] = i;
                parent = info[POST2_PARENT][parent];
            }
            parentR = info[RKR][i];
            while (parentR > -1 && info[RPOST2_MIN_RKR][parentR] == -1) {
                info[RPOST2_MIN_RKR][parentR] = i;
                parentR = info[POST2_PARENT][info[RPOST2_POST][parentR]]; // get parent's postorder
                if (parentR > -1) {
                    parentR = treeSize - 1 - info[POST2_PRE][parentR]; // if parent exists get its rev. postorder
                }
            }
        }
    }
    
    /**
     * Transforms a list of Integer objects to an array of primitive int values.
     * @param integers
     * @return 
     */
    public static int[] toIntArray(List<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }

    public void setSwitched(boolean value) {
        switched = value;
    }
    
    public boolean isSwitched() {
        return switched;
    }
}
