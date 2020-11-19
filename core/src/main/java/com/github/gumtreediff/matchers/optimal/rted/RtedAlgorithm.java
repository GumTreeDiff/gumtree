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

import java.util.*;

import com.github.gumtreediff.tree.Tree;


/**
 * Computes the tree edit distance using RTED algorithm.
 * 
 * @author Mateusz Pawlik, Nikolaus Augsten
 */
public class RtedAlgorithm {

	// constants
	private static final byte LEFT = 0;
	private static final byte RIGHT = 1;
	private static final byte HEAVY = 2;
	private static final byte BOTH = 3;
	private static final byte REVLEFT = 4;
	private static final byte REVRIGHT = 5;
	private static final byte REVHEAVY = 6;
	private static final byte POST2_SIZE = 0;
	private static final byte POST2_KR_SUM = 1;
	private static final byte POST2_REV_KR_SUM = 2;
	private static final byte POST2_DESC_SUM = 3; // number of subforests in
													// full decomposition
	private static final byte POST2_PRE = 4;
	private static final byte POST2_PARENT = 5;
	private static final byte POST2_LABEL = 6;
	private static final byte KR = 7; // key root nodes (size of this array =
										// leaf count)
	private static final byte POST2_LLD = 8; // left-most leaf descendants
	private static final byte POST2_MIN_KR = 9; // minimum key root nodes index
												// in KR array
	private static final byte RKR = 10; // reversed key root nodes
	private static final byte RPOST2_RLD = 11; // reversed postorer 2 right-most
												// leaf descendants
	private static final byte RPOST2_MIN_RKR = 12; // minimum key root nodes
													// index in RKR array
	private static final byte RPOST2_POST = 13; // reversed postorder ->
												// postorder
	private static final byte POST2_STRATEGY = 14; // strategy for Demaine (is
													// there sth on the
													// left/right of the heavy
													// node)
	private static final byte PRE2_POST = 15; // preorder to postorder

	// trees
	private InfoTree it1;
	private InfoTree it2;
	private int size1;
	private int size2;
	private LabelDictionary ld;

	// arrays
	private int[][] str; // strategy array
	private double[][] delta; // an array for storing the distances between
								// every pair of subtrees
	private byte[][] deltaBit; // stores the distances difference of a form
								// delta(F,G)-delta(F°,G°) for every pair of
								// subtrees, which is at most 1
	private int[][] ij; // stores a forest preorder for given i and j
	private long[][][] costV;
	private long[][] costW;
	private double[][] t; // T array from Demaine's algorithm, stores
							// delta(Fv,Gij), v on heavy path. Values are
							// written to t.
	private double[][] tCOPY; // tCOPY serves for writing values. It may happen
								// that in single computePeriod values are
								// overwritten before they are read because of
								// the change of forest ordering.
	private double[][] tTMP;
	private double[][] s;
	private double[] q;
	
	private double da, db, dc;
	private int previousStrategy;
	private int[] strStat = new int[5]; // statistics for strategies
										// LEFT,RIGHT,HEAVY,SUM
	private double costDel, costIns, costMatch; // edit operations costs

	/**
	 * The constructor. Parameters passed are the edit operation costs.
	 * 
	 * @param delCost
	 * @param insCost
	 * @param matchCost
	 */
	public RtedAlgorithm(double delCost, double insCost, double matchCost) {
		this.costDel = delCost;
		this.costIns = insCost;
		this.costMatch = matchCost;
	}

	public double nonNormalizedTreeDist() {
		if (it1 == null || it2 == null) {
			System.err.println("No stored trees to compare.");
		}
		if (str == null) {
			System.err.println("No strategy to use.");
		}
		return computeDistUsingStrArray(it1, it2);
	}

	public void init(Tree src, Tree dst) {
		ld = new LabelDictionary();
		it1 = new InfoTree(src, ld);
		it2 = new InfoTree(dst, ld);
		size1 = it1.getSize();
		size2 = it2.getSize();
		ij = new int[Math.max(size1, size2)][Math.max(size1, size2)];
		delta = new double[size1][size2];
		deltaBit = new byte[size1][size2];
		costV = new long[3][size1][size2];
		costW = new long[3][size2];

		// Calculate delta between every leaf in G (empty tree) and all the
		// nodes in F.
		// Calculate it both sides: leafs of F and nodes of G & leafs of G and
		// nodes of F.
		int[] labels1 = it1.getInfoArray(POST2_LABEL);
		int[] labels2 = it2.getInfoArray(POST2_LABEL);
		int[] sizes1 = it1.getInfoArray(POST2_SIZE);
		int[] sizes2 = it2.getInfoArray(POST2_SIZE);
		for (int x = 0; x < sizes1.length; x++) { // for all nodes of initially
													// left tree
			for (int y = 0; y < sizes2.length; y++) { // for all nodes of
														// initially right tree

				// This is an attempt for distances of single-node subtree and
				// anything alse
				// The differences between pairs of labels are stored
				if (labels1[x] == labels2[y]) {
					deltaBit[x][y] = 0;
				} else {
					deltaBit[x][y] = 1; // if this set, the labels differ, cost
										// of relabeling is set to costMatch
				}

				if (sizes1[x] == 1 && sizes2[y] == 1) { // both nodes are leafs
					delta[x][y] = 0;
				} else {
					if (sizes1[x] == 1) {
						delta[x][y] = sizes2[y] - 1;
					}
					if (sizes2[y] == 1) {
						delta[x][y] = sizes1[x] - 1;
					}
				}
			}
		}
	}

	/**
	 * A method for computing and storing the optimal strategy
	 */
	public void computeOptimalStrategy() {
		long heavyMin, revHeavyMin, leftMin, revLeftMin, rightMin, revRightMin;
		long min = -1;
		int strategy = -1;
		int parent1 = -1;
		int parent2 = -1;
		boolean[] nodeTypeLeft1 = it1.nodeType[LEFT];
		boolean[] nodeTypeLeft2 = it2.nodeType[LEFT];
		boolean[] nodeTypeRigt1 = it1.nodeType[RIGHT];
		boolean[] nodeTypeRight2 = it2.nodeType[RIGHT];
		boolean[] nodeTypeHeavy1 = it1.nodeType[HEAVY];
		boolean[] nodeTypeHeavy2 = it2.nodeType[HEAVY];
		int[] post2size1 = it1.info[POST2_SIZE];
		int[] post2size2 = it2.info[POST2_SIZE];
		int[] post2descSum1 = it1.info[POST2_DESC_SUM];
		int[] post2descSum2 = it2.info[POST2_DESC_SUM];
		int[] post2krSum1 = it1.info[POST2_KR_SUM];
		int[] post2krSum2 = it2.info[POST2_KR_SUM];
		int[] post2revkrSum1 = it1.info[POST2_REV_KR_SUM];
		int[] post2revkrSum2 = it2.info[POST2_REV_KR_SUM];
		int[] post2parent1 = it1.info[POST2_PARENT];
		int[] post2parent2 = it2.info[POST2_PARENT];

		str = new int[size1][size2];

		// v represents nodes of left input tree in postorder
		// w represents nodes of right input tree in postorder
		for (int v = 0; v < size1; v++) {
			Arrays.fill(costW[0], 0);
			Arrays.fill(costW[1], 0);
			Arrays.fill(costW[2], 0);
			for (int w = 0; w < size2; w++) {
				if (post2size2[w] == 1) {
					// putTree zeros into arrays
					costW[LEFT][w] = 0;
					costW[RIGHT][w] = 0;
					costW[HEAVY][w] = 0;
				}
				if (post2size1[v] == 1) {
					// putTree zeros into arrays
					costV[LEFT][v][w] = 0;
					costV[RIGHT][v][w] = 0;
					costV[HEAVY][v][w] = 0;
				}

				// TODO: some things below may be putTree to outer loop

				// count the minimum + get the strategy
				heavyMin = (long) post2size1[v] * (long) post2descSum2[w]
						+ costV[HEAVY][v][w];
				revHeavyMin = (long) post2size2[w] * (long) post2descSum1[v]
						+ costW[HEAVY][w];

				leftMin = (long) post2size1[v] * (long) post2krSum2[w]
						+ costV[LEFT][v][w];
				revLeftMin = (long) post2size2[w] * (long) post2krSum1[v]
						+ costW[LEFT][w];

				rightMin = (long) post2size1[v] * (long) post2revkrSum2[w]
						+ costV[RIGHT][v][w];
				revRightMin = (long) post2size2[w] * (long) post2revkrSum1[v]
						+ costW[RIGHT][w];

				long[] mins = { leftMin, rightMin, heavyMin, Long.MAX_VALUE,
						revLeftMin, revRightMin, revHeavyMin };

				min = leftMin;
				strategy = 0;
				for (int i = 1; i <= 6; i++) {
					if (mins[i] < min) {
						min = mins[i];
						strategy = i;
					}
				}

				// store the strategy for the minimal cost
				str[v][w] = strategy;

				// fill the cost arrays
				parent1 = post2parent1[v];
				if (parent1 != -1) {
					costV[HEAVY][parent1][w] += nodeTypeHeavy1[v] ? costV[HEAVY][v][w]
							: min;
					costV[RIGHT][parent1][w] += nodeTypeRigt1[v] ? costV[RIGHT][v][w]
							: min;
					costV[LEFT][parent1][w] += nodeTypeLeft1[v] ? costV[LEFT][v][w]
							: min;
				}
				parent2 = post2parent2[w];
				if (parent2 != -1) {
					costW[HEAVY][parent2] += nodeTypeHeavy2[w] ? costW[HEAVY][w]
							: min;
					costW[LEFT][parent2] += nodeTypeLeft2[w] ? costW[LEFT][w]
							: min;
					costW[RIGHT][parent2] += nodeTypeRight2[w] ? costW[RIGHT][w]
							: min;
				}
			}
		}
	}

	/**
	 * The recursion step according to the optimal strategy.
	 * 
	 * @param it1
	 * @param it2
	 * @return
	 */
	private double computeDistUsingStrArray(InfoTree it1, InfoTree it2) {

		int postorder1 = it1.getCurrentNode();
		int postorder2 = it2.getCurrentNode();

		int stepStrategy = str[postorder1][postorder2];

		int tmpPostorder;

		int[] stepPath;
		int[] stepRelSubtrees;
		ArrayList<Integer> heavyPath;
		switch (stepStrategy) {
		case LEFT:
			tmpPostorder = postorder1;
			stepPath = it1.getPath(LEFT);
			// go along the path
			while (stepPath[postorder1] > -1) {
				stepRelSubtrees = it1.getNodeRelSubtrees(LEFT, postorder1);
				if (stepRelSubtrees != null) {
					// iterate over rel subtrees for a specific node on the path
					for (int rs : stepRelSubtrees) {
						it1.setCurrentNode(rs);
						// make the recursion
						computeDistUsingStrArray(it1, it2);
					}
				}
				postorder1 = stepPath[postorder1];
			}
			// set current node
			it1.setCurrentNode(tmpPostorder);
			it1.setSwitched(false);
			it2.setSwitched(false);
			// count the distance using a single-path function
			strStat[3]++;
			strStat[LEFT]++;
			return spfL(it1, it2);
		case RIGHT:
			tmpPostorder = postorder1;
			stepPath = it1.getPath(RIGHT);
			while (stepPath[postorder1] > -1) {
				stepRelSubtrees = it1.getNodeRelSubtrees(RIGHT, postorder1);
				if (stepRelSubtrees != null) {
					for (int rs : stepRelSubtrees) {
						it1.setCurrentNode(rs);
						computeDistUsingStrArray(it1, it2);
					}
				}
				postorder1 = stepPath[postorder1];
			}
			it1.setCurrentNode(tmpPostorder);
			it1.setSwitched(false);
			it2.setSwitched(false);
			strStat[3]++;
			strStat[RIGHT]++;
			return spfR(it1, it2);
		case HEAVY:
			tmpPostorder = postorder1;
			stepPath = it1.getPath(HEAVY);
			heavyPath = new ArrayList<Integer>();
			heavyPath.add(postorder1);
			while (stepPath[postorder1] > -1) {
				stepRelSubtrees = it1.getNodeRelSubtrees(HEAVY, postorder1);
				if (stepRelSubtrees != null) {
					for (int rs : stepRelSubtrees) {
						it1.setCurrentNode(rs);
						computeDistUsingStrArray(it1, it2);
					}
				}
				postorder1 = stepPath[postorder1];
				heavyPath.add(postorder1);
			}
			it1.setCurrentNode(tmpPostorder);
			it1.setSwitched(false);
			it2.setSwitched(false);
			strStat[3]++;
			strStat[HEAVY]++;
			return spfH(it1, it2, InfoTree.toIntArray(heavyPath));
		case REVLEFT:
			tmpPostorder = postorder2;
			stepPath = it2.getPath(LEFT);
			while (stepPath[postorder2] > -1) {
				stepRelSubtrees = it2.getNodeRelSubtrees(LEFT, postorder2);
				if (stepRelSubtrees != null) {
					for (int rs : stepRelSubtrees) {
						it2.setCurrentNode(rs);
						computeDistUsingStrArray(it1, it2);
					}
				}
				postorder2 = stepPath[postorder2];
			}
			it2.setCurrentNode(tmpPostorder);
			it1.setSwitched(true);
			it2.setSwitched(true);
			strStat[3]++;
			strStat[LEFT]++;
			return spfL(it2, it1);
		case REVRIGHT:
			tmpPostorder = postorder2;
			stepPath = it2.getPath(RIGHT);
			while (stepPath[postorder2] > -1) {
				stepRelSubtrees = it2.getNodeRelSubtrees(RIGHT, postorder2);
				if (stepRelSubtrees != null) {
					for (int rs : stepRelSubtrees) {
						it2.setCurrentNode(rs);
						computeDistUsingStrArray(it1, it2);
					}
				}
				postorder2 = stepPath[postorder2];
			}
			it2.setCurrentNode(tmpPostorder);
			it1.setSwitched(true);
			it2.setSwitched(true);
			strStat[3]++;
			strStat[RIGHT]++;
			return spfR(it2, it1);
		case REVHEAVY:
			tmpPostorder = postorder2;
			stepPath = it2.getPath(HEAVY);
			heavyPath = new ArrayList<Integer>();
			heavyPath.add(postorder2);
			while (stepPath[postorder2] > -1) {
				stepRelSubtrees = it2.getNodeRelSubtrees(HEAVY, postorder2);
				if (stepRelSubtrees != null) {
					for (int rs : stepRelSubtrees) {
						it2.setCurrentNode(rs);
						computeDistUsingStrArray(it1, it2);
					}
				}
				postorder2 = stepPath[postorder2];
				heavyPath.add(postorder2);
			}
			it2.setCurrentNode(tmpPostorder);
			it1.setSwitched(true);
			it2.setSwitched(true);
			strStat[3]++;
			strStat[HEAVY]++;
			return spfH(it2, it1, InfoTree.toIntArray(heavyPath));
		default:
			return -1;
		}
	}

	/**
	 * Single-path function for the left-most path based on Zhang and Shasha
	 * algorithm.
	 * 
	 * @param it1
	 * @param it2
	 * @return distance between subtrees it1 and it2
	 */
	private double spfL(InfoTree it1, InfoTree it2) {

		int fPostorder = it1.getCurrentNode();
		int gPostorder = it2.getCurrentNode();

		int minKR = it2.info[POST2_MIN_KR][gPostorder];
		int[] kr = it2.info[KR];
		if (minKR > -1) {
			for (int j = minKR; kr[j] < gPostorder; j++) {
				treeEditDist(it1, it2, fPostorder, kr[j]);
			}
		}
		treeEditDist(it1, it2, fPostorder, gPostorder);

		return it1.isSwitched() ? delta[gPostorder][fPostorder]
				+ deltaBit[gPostorder][fPostorder] * costMatch
				: delta[fPostorder][gPostorder]
						+ deltaBit[fPostorder][gPostorder] * costMatch;
	}

	private void treeEditDist(InfoTree it1, InfoTree it2, int i, int j) {
		int m = i - it1.info[POST2_LLD][i] + 2;
		int n = j - it2.info[POST2_LLD][j] + 2;
		double[][] forestdist = new double[m][n];
		int ioff = it1.info[POST2_LLD][i] - 1;
		int joff = it2.info[POST2_LLD][j] - 1;
		boolean switched = it1.isSwitched();
		forestdist[0][0] = 0;
		for (int i1 = 1; i1 <= i - ioff; i1++) forestdist[i1][0] = forestdist[i1 - 1][0] + 1;
		for (int j1 = 1; j1 <= j - joff; j1++) forestdist[0][j1] = forestdist[0][j1 - 1] + 1;
		for (int i1 = 1; i1 <= i - ioff; i1++) {
			for (int j1 = 1; j1 <= j - joff; j1++) {
				if ((it1.info[POST2_LLD][i1 + ioff] == it1.info[POST2_LLD][i]) && (it2.info[POST2_LLD][j1 + joff] == it2.info[POST2_LLD][j])) {
					double u = 0;
					if (it1.info[POST2_LABEL][i1 + ioff] != it2.info[POST2_LABEL][j1 + joff]) u = costMatch;
					da = forestdist[i1 - 1][j1] + costDel;
					db = forestdist[i1][j1 - 1] + costIns;
					dc = forestdist[i1 - 1][j1 - 1] + u;
					forestdist[i1][j1] = (da < db) ? ((da < dc) ? da : dc) : ((db < dc) ? db : dc);
					setDeltaValue(i1 + ioff, j1 + joff, forestdist[i1 - 1][j1 - 1], switched);
					setDeltaBitValue(i1 + ioff, j1 + joff, (byte) ((forestdist[i1][j1] - forestdist[i1 - 1][j1 - 1] > 0) ? 1 : 0), switched);
				} else {
					double u = 0;
					u = switched ? deltaBit[j1 + joff][i1 + ioff] * costMatch : deltaBit[i1 + ioff][j1 + joff] * costMatch;
					da = forestdist[i1 - 1][j1] + costDel;
					db = forestdist[i1][j1 - 1] + costIns;
					dc = forestdist[it1.info[POST2_LLD][i1 + ioff] - 1 - ioff][it2.info[POST2_LLD][j1 + joff]
							- 1 - joff] + (switched ? delta[j1 + joff][i1 + ioff] : delta[i1 + ioff][j1 + joff]) + u;
					forestdist[i1][j1] = (da < db) ? ((da < dc) ? da : dc) : ((db < dc) ? db : dc);
				}
			}
		}
	}

	/**
	 * Single-path function for right-most path based on symmetric version of
	 * Zhang and Shasha algorithm.
	 * 
	 * @param it1
	 * @param it2
	 * @return distance between subtrees it1 and it2
	 */
	private double spfR(InfoTree it1, InfoTree it2) {

		int fReversedPostorder = it1.getSize() - 1 - it1.info[POST2_PRE][it1.getCurrentNode()];
		int gReversedPostorder = it2.getSize() - 1 - it2.info[POST2_PRE][it2.getCurrentNode()];

		int minRKR = it2.info[RPOST2_MIN_RKR][gReversedPostorder];
		int[] rkr = it2.info[RKR];
		if (minRKR > -1) for (int j = minRKR; rkr[j] < gReversedPostorder; j++) treeEditDistRev(it1, it2, fReversedPostorder, rkr[j]);
		treeEditDistRev(it1, it2, fReversedPostorder, gReversedPostorder);

		return it1.isSwitched() ? delta[it2.getCurrentNode()][it1
				.getCurrentNode()]
				+ deltaBit[it2.getCurrentNode()][it1.getCurrentNode()]
				* costMatch : delta[it1.getCurrentNode()][it2.getCurrentNode()]
				+ deltaBit[it1.getCurrentNode()][it2.getCurrentNode()]
				* costMatch;
	}

	private void treeEditDistRev(InfoTree it1, InfoTree it2, int i, int j) {
		int m = i - it1.info[RPOST2_RLD][i] + 2;
		int n = j - it2.info[RPOST2_RLD][j] + 2;
		double[][] forestdist = new double[m][n];
		int ioff = it1.info[RPOST2_RLD][i] - 1;
		int joff = it2.info[RPOST2_RLD][j] - 1;
		boolean switched = it1.isSwitched();
		forestdist[0][0] = 0;
		
		for (int i1 = 1; i1 <= i - ioff; i1++) forestdist[i1][0] = forestdist[i1 - 1][0] + 1;
		for (int j1 = 1; j1 <= j - joff; j1++) forestdist[0][j1] = forestdist[0][j1 - 1] + 1;
		for (int i1 = 1; i1 <= i - ioff; i1++) {
			for (int j1 = 1; j1 <= j - joff; j1++) {
				if ((it1.info[RPOST2_RLD][i1 + ioff] == it1.info[RPOST2_RLD][i])
						&& (it2.info[RPOST2_RLD][j1 + joff] == it2.info[RPOST2_RLD][j])) {
					double u = 0;
					if (it1.info[POST2_LABEL][it1.info[RPOST2_POST][i1 + ioff]] != it2.info[POST2_LABEL][it2.info[RPOST2_POST][j1 + joff]])
						u = costMatch;
					
					da = forestdist[i1 - 1][j1] + costDel;
					db = forestdist[i1][j1 - 1] + costIns;
					dc = forestdist[i1 - 1][j1 - 1] + u;
					forestdist[i1][j1] = (da < db) ? ((da < dc) ? da : dc) : ((db < dc) ? db : dc);

					setDeltaValue(it1.info[RPOST2_POST][i1 + ioff], it2.info[RPOST2_POST][j1 + joff], forestdist[i1 - 1][j1 - 1], switched);
					setDeltaBitValue(it1.info[RPOST2_POST][i1 + ioff], it2.info[RPOST2_POST][j1 + joff],
							(byte) ((forestdist[i1][j1] - forestdist[i1 - 1][j1 - 1] > 0) ? 1 : 0), switched);
				} else {
					double u = 0;
					u = switched ? deltaBit[it2.info[RPOST2_POST][j1 + joff]][it1.info[RPOST2_POST][i1
							+ ioff]]
							* costMatch
							: deltaBit[it1.info[RPOST2_POST][i1 + ioff]][it2.info[RPOST2_POST][j1
									+ joff]]
									* costMatch;

					da = forestdist[i1 - 1][j1] + costDel;
					db = forestdist[i1][j1 - 1] + costIns;
					dc = forestdist[it1.info[RPOST2_RLD][i1 + ioff] - 1 - ioff][it2.info[RPOST2_RLD][j1
							+ joff]
							- 1 - joff]
							+ (switched ? delta[it2.info[RPOST2_POST][j1 + joff]][it1.info[RPOST2_POST][i1
									+ ioff]]
									: delta[it1.info[RPOST2_POST][i1 + ioff]][it2.info[RPOST2_POST][j1
											+ joff]]) + u;
					forestdist[i1][j1] = (da < db) ? ((da < dc) ? da : dc)
							: ((db < dc) ? db : dc);
				}
			}
		}

	}

	/**
	 * Single-path function for heavy path based on Klein/Demaine algorithm.
	 * 
	 * @param it1
	 * @param it2
	 * @param heavyPath
	 * @return distance between subtrees it1 and it2
	 */
	private double spfH(InfoTree it1, InfoTree it2, int[] heavyPath) {

		int fSize = it1.info[POST2_SIZE][it1.getCurrentNode()];
		int gSize = it2.info[POST2_SIZE][it2.getCurrentNode()];

		int gRevPre = it2.getSize() - 1 - it2.getCurrentNode();
		int gPre = it2.info[POST2_PRE][it2.getCurrentNode()];

		int gTreeSize = it2.getSize();

		int strategy;

		int jOfi;

		// Initialize arrays to their maximal possible size for current pairs of
		// subtrees.
		t = new double[gSize][gSize];
		tCOPY = new double[gSize][gSize];
		s = new double[fSize][gSize];
		q = new double[fSize];

		int vp = -1;
		int nextVp = -1;

		for (int it = heavyPath.length - 1; it >= 0; it--) {
			vp = heavyPath[it];
			strategy = it1.info[POST2_STRATEGY][vp];
			if (strategy != BOTH) {
				if (it1.info[POST2_SIZE][vp] == 1) {
					for (int i = gSize - 1; i >= 0; i--) {
						jOfi = jOfI(it2, i, gSize, gRevPre, gPre, strategy,
								gTreeSize);
						for (int j = jOfi; j >= 0; j--) {
							t[i][j] = (gSize - (i + j)) * costIns;
						}
					}
					previousStrategy = strategy;
				}
				computePeriod(it1, vp, nextVp, it2, strategy);
			} else {
				if (it1.info[POST2_SIZE][vp] == 1) {
					for (int i = gSize - 1; i >= 0; i--) {
						jOfi = jOfI(it2, i, gSize, gRevPre, gPre, LEFT,
								gTreeSize);
						for (int j = jOfi; j >= 0; j--) {
							t[i][j] = (gSize - (i + j)) * costIns;
						}
					}
					previousStrategy = LEFT;
				}
				computePeriod(it1, vp, nextVp, it2, LEFT);
				if (it1.info[POST2_SIZE][vp] == 1) {
					for (int i = gSize - 1; i >= 0; i--) {
						jOfi = jOfI(it2, i, gSize, gRevPre, gPre, RIGHT,
								gTreeSize);
						for (int j = jOfi; j >= 0; j--) {
							t[i][j] = (gSize - (i + j)) * costIns;
						}
					}
					previousStrategy = RIGHT;
				}
				computePeriod(it1, vp, nextVp, it2, RIGHT);
			}
			nextVp = vp;
		}
		return t[0][0];
	}

	/**
	 * Compute period method.
	 * 
	 * @param it1
	 * @param aVp
	 * @param aNextVp
	 * @param it2
	 * @param aStrategy
	 */
	private void computePeriod(InfoTree it1, int aVp, int aNextVp, InfoTree it2, int aStrategy) {
		int fTreeSize = it1.getSize();
		int gTreeSize = it2.getSize();

		int vpPreorder = it1.info[POST2_PRE][aVp];
		int vpRevPreorder = fTreeSize - 1 - aVp;
		int vpSize = it1.info[POST2_SIZE][aVp];

		int gSize = it2.info[POST2_SIZE][it2.getCurrentNode()];
		int gPreorder = it2.info[POST2_PRE][it2.getCurrentNode()];
		int gRevPreorder = gTreeSize - 1 - it2.getCurrentNode();

		int nextVpPreorder = -1;
		int nextVpRevPreorder = -1;
		int nextVpSize = -1;
		// count k and assign next vp values
		int k;
		if (aNextVp != -1) {
			nextVpPreorder = it1.info[POST2_PRE][aNextVp];
			nextVpRevPreorder = fTreeSize - 1 - aNextVp;
			nextVpSize = it1.info[POST2_SIZE][aNextVp];
			// if strategy==LEFT use preorder to count number of left deletions
			// from vp to vp-1
			// if strategy==RIGHT use reversed preorder
			k = aStrategy == LEFT ? nextVpPreorder - vpPreorder
					: nextVpRevPreorder - vpRevPreorder;
			if (aStrategy != previousStrategy) {
				computeIJTable(it2, gPreorder, gRevPreorder, gSize, aStrategy,
						gTreeSize);
			}
		} else {
			k = 1;
			computeIJTable(it2, gPreorder, gRevPreorder, gSize, aStrategy, gTreeSize);
		}

		int realStrategy = it1.info[POST2_STRATEGY][aVp];
		boolean switched = it1.isSwitched();
		tTMP = tCOPY;
		tCOPY = t;
		t = tTMP;

		// if aVp is a leaf => precompute table T - edit distance betwen EMPTY
		// and all subforests of G

		// check if nextVp is the only child of vp
		if (vpSize - nextVpSize == 1) {
			// update delta from T table => dist between Fvp-1 and G was
			// computed in previous compute period
			if (gSize == 1) {
				setDeltaValue(it1.info[PRE2_POST][vpPreorder], it2.info[PRE2_POST][gPreorder], vpSize - 1, switched);
			} else {
				setDeltaValue(it1.info[PRE2_POST][vpPreorder], it2.info[PRE2_POST][gPreorder], t[1][0], switched);
			}
		}

		int gijForestPreorder;
		int previousI;
		int fForestPreorderKPrime;
		int jPrime;
		int kBis;
		int jOfIminus1;
		int gijOfIMinus1Preorder;
		int jOfI;
		double deleteFromLeft;
		double deleteFromRight;
		double match;
		int fLabel;
		int gLabel;

		// Q and T are visible for every i
		for (int i = gSize - 1; i >= 0; i--) {

			// jOfI was already computed once in spfH
			jOfI = jOfI(it2, i, gSize, gRevPreorder, gPreorder, aStrategy,
					gTreeSize);

			// when strategy==BOTH first LEFT then RIGHT is done

			//counter += realStrategy == BOTH && aStrategy == LEFT ? (k - 1)
					//* (jOfI + 1) : k * (jOfI + 1);

			// S - visible for current i

			for (int kPrime = 1; kPrime <= k; kPrime++) {

				fForestPreorderKPrime = aStrategy == LEFT ? vpPreorder
						+ (k - kPrime) : it1.info[POST2_PRE][fTreeSize - 1
						- (vpRevPreorder + (k - kPrime))];
				kBis = kPrime
						- it1.info[POST2_SIZE][it1.info[PRE2_POST][fForestPreorderKPrime]];

				// reset the minimum arguments' values
				deleteFromRight = costIns;
				deleteFromLeft = costDel;
				match = 0;

				match += aStrategy == LEFT ? kBis + nextVpSize : vpSize - k
						+ kBis;

				if ((i + jOfI) == (gSize - 1)) {
					deleteFromRight += (vpSize - (k - kPrime));
				} else {
					deleteFromRight += q[kPrime - 1];
				}

				fLabel = it1.info[POST2_LABEL][it1.info[PRE2_POST][fForestPreorderKPrime]];

				for (int j = jOfI; j >= 0; j--) {
					// count dist(FkPrime, Gij) with min

					// delete from left

					gijForestPreorder = aStrategy == LEFT ? ij[i][j]
							: it2.info[POST2_PRE][gTreeSize - 1 - ij[i][j]];

					if (kPrime == 1) {
						// if the direction changed from the previous period to
						// this one use i and j of previous strategy

						// since T is overwritten continuously, thus use copy of
						// T for getting values from previous period

						if (aStrategy != previousStrategy) {
							if (aStrategy == LEFT) {
								previousI = gijForestPreorder - gPreorder; // minus
																			// preorder
																			// of
																			// G
							} else {
								previousI = gTreeSize
										- 1
										- it2.info[RPOST2_POST][gTreeSize - 1
												- gijForestPreorder]
										- gRevPreorder; // minus rev preorder of
														// G
							}
							deleteFromLeft += tCOPY[previousI][i + j
									- previousI];
						} else {
							deleteFromLeft += tCOPY[i][j];
						}

					} else {
						deleteFromLeft += s[kPrime - 1 - 1][j];
					}

					// match

					match += switched ? delta[it2.info[PRE2_POST][gijForestPreorder]][it1.info[PRE2_POST][fForestPreorderKPrime]]
							: delta[it1.info[PRE2_POST][fForestPreorderKPrime]][it2.info[PRE2_POST][gijForestPreorder]];

					jPrime = j
							+ it2.info[POST2_SIZE][it2.info[PRE2_POST][gijForestPreorder]];

					// if root nodes of L/R Fk' and L/R Gij have different
					// labels add the match cost
					gLabel = it2.info[POST2_LABEL][it2.info[PRE2_POST][gijForestPreorder]];

					if (fLabel != gLabel) {
						match += costMatch;
					}

					// this condition is checked many times but is not satisfied
					// only once
					if (j != jOfI) {
						// delete from right
						deleteFromRight += s[kPrime - 1][j + 1];
						if (kBis == 0) {
							if (aStrategy != previousStrategy) {
								previousI = aStrategy == LEFT ? ij[i][jPrime]
										- gPreorder : ij[i][jPrime]
										- gRevPreorder;
								match += tCOPY[previousI][i + jPrime
										- previousI];
							} else {
								match += tCOPY[i][jPrime];
							}
						} else if (kBis > 0) {
							match += s[kBis - 1][jPrime];
						} else {
							match += gSize - (i + jPrime);
						}

					}

					// fill S table
					s[kPrime - 1][j] = (deleteFromLeft < deleteFromRight) ? ((deleteFromLeft < match) ? deleteFromLeft
							: match)
							: ((deleteFromRight < match) ? deleteFromRight
									: match);

					// reset the minimum arguments' values
					deleteFromRight = costIns;
					deleteFromLeft = costDel;
					match = 0;
				}
			}

			// compute table T => add row to T
			// if (realStrategy == BOTH && aStrategy == LEFT) {
			// // t[i] has to be of correct length
			// // assigning pointer of a row in s to t[i] is wrong
			// // t[i] = Arrays.copyOf(s[k-1-1], jOfI+1);//sTable[k - 1 - 1];
			// // System.arraycopy(s[k-1-1], 0, t[i], 0, jOfI+1);
			// t[i] = s[k-1-1].clone();
			// } else {
			// // t[i] = Arrays.copyOf(s[k-1], jOfI+1);//sTable[k - 1];
			// // System.arraycopy(s[k-1], 0, t[i], 0, jOfI+1);
			// t[i] = s[k-1].clone();
			// }

			// compute table T => add row to T
			// we have to copy the values, otherwise they may be overwritten t
			// early
			t[i] = (realStrategy == BOTH && aStrategy == LEFT) ? s[k - 1 - 1]
					.clone() : s[k - 1].clone();

			if (i > 0) {
				// compute table Q
				jOfIminus1 = jOfI(it2, i - 1, gSize, gRevPreorder, gPreorder,
						aStrategy, gTreeSize);
				if (jOfIminus1 <= jOfI) {
					for (int x = 0; x < k; x++) { // copy whole column |
													// qTable.length=k
						q[x] = s[x][jOfIminus1];
					}
				}

				// fill table delta
				if (i + jOfIminus1 < gSize) {

					gijOfIMinus1Preorder = aStrategy == LEFT ? it2.info[POST2_PRE][gTreeSize
							- 1 - (gRevPreorder + (i - 1))]
							: gPreorder + (i - 1);

					// If Fk from Fk-1 differ with a single node,
					// then Fk without the root node is Fk-1 and the distance
					// value has to be taken from previous T table.
					if (k - 1 - 1 < 0) {
						if (aStrategy != previousStrategy) {
							previousI = aStrategy == LEFT ? ij[i][jOfIminus1]
									- gPreorder : ij[i][jOfIminus1]
									- gRevPreorder;
							setDeltaValue(
									it1.info[PRE2_POST][vpPreorder],
									it2.info[PRE2_POST][gijOfIMinus1Preorder],
									tCOPY[previousI][i + jOfIminus1 - previousI],
									switched);
						} else {
							setDeltaValue(it1.info[PRE2_POST][vpPreorder],
									it2.info[PRE2_POST][gijOfIMinus1Preorder],
									tCOPY[i][jOfIminus1], switched);
						}
					} else {
						setDeltaValue(it1.info[PRE2_POST][vpPreorder],
								it2.info[PRE2_POST][gijOfIMinus1Preorder],
								s[k - 1 - 1][jOfIminus1], switched);
					}
				}
			}

		}
		previousStrategy = aStrategy;
	}

	/**
	 * Computes an array where preorder/rev.preorder of a subforest of given
	 * subtree is stored and can be accessed for given i and j.
	 * 
	 * @param it
	 * @param subtreePreorder
	 * @param subtreeRevPreorder
	 * @param subtreeSize
	 * @param aStrategy
	 * @param treeSize
	 */
	private void computeIJTable(InfoTree it, int subtreePreorder,
			int subtreeRevPreorder, int subtreeSize, int aStrategy, int treeSize) {

		int change;

		int[] post2pre = it.info[POST2_PRE];
		int[] rpost2post = it.info[RPOST2_POST];

		if (aStrategy == LEFT) {
			for (int x = 0; x < subtreeSize; x++) {
				ij[0][x] = x + subtreePreorder;
			}
			for (int x = 1; x < subtreeSize; x++) {
				change = post2pre[(treeSize - 1 - (x - 1 + subtreeRevPreorder))];
				for (int z = 0; z < subtreeSize; z++) {
					if (ij[x - 1][z] >= change) {
						ij[x][z] = ij[x - 1][z] + 1;
					} else {
						ij[x][z] = ij[x - 1][z];
					}
				}
			}
		} else { // if (aStrategy == RIGHT) {
			for (int x = 0; x < subtreeSize; x++) {
				ij[0][x] = x + subtreeRevPreorder;
			}
			for (int x = 1; x < subtreeSize; x++) {
				change = treeSize
						- 1
						- rpost2post[(treeSize - 1 - (x - 1 + subtreePreorder))];
				for (int z = 0; z < subtreeSize; z++) {
					if (ij[x - 1][z] >= change) {
						ij[x][z] = ij[x - 1][z] + 1;
					} else {
						ij[x][z] = ij[x - 1][z];
					}
				}
			}
		}
	}

	/**
	 * Returns j for given i, result of j(i) form Demaine's algorithm.
	 * 
	 * @param it
	 * @param aI
	 * @param aSubtreeWeight
	 * @param aSubtreeRevPre
	 * @param aSubtreePre
	 * @param aStrategy
	 * @param treeSize
	 * @return j for given i
	 */
	private int jOfI(InfoTree it, int aI, int aSubtreeWeight,
			int aSubtreeRevPre, int aSubtreePre, int aStrategy, int treeSize) {
		return aStrategy == LEFT ? aSubtreeWeight - aI
				- it.info[POST2_SIZE][treeSize - 1 - (aSubtreeRevPre + aI)]
				: aSubtreeWeight
						- aI
						- it.info[POST2_SIZE][it.info[RPOST2_POST][treeSize - 1
								- (aSubtreePre + aI)]];
	}

	private void setDeltaValue(int a, int b, double value, boolean switched) {
		if (switched) {
			delta[b][a] = value;
		} else {
			delta[a][b] = value;
		}
	}

	private void setDeltaBitValue(int a, int b, byte value, boolean switched) {
		if (switched) {
			deltaBit[b][a] = value;
		} else {
			deltaBit[a][b] = value;
		}
	}

	public void setCustomCosts(double costDel, double costIns, double costMatch) {
		this.costDel = costDel;
		this.costIns = costIns;
		this.costMatch = costMatch;
	}

	public void setCustomStrategy(int[][] strategyArray) {
		str = strategyArray;
	}

	public void setCustomStrategy(int strategy, boolean ifSwitch) {
		str = new int[size1][size2];
		if (ifSwitch) {
			for (int i = 0; i < size1; i++) {
				for (int j = 0; j < size2; j++) {
					str[i][j] = it1.info[POST2_SIZE][i] >= it2.info[POST2_SIZE][j] ? strategy
							: strategy + 4;
				}
			}
		} else {
			for (int i = 0; i < size1; i++) {
				Arrays.fill(str[i], strategy);
			}
		}
	}

	/**
	 * Compute the minimal edit mapping between two trees. There might be
	 * multiple minimal edit mappings. This function computes only one of them.
	 * 
	 * The first step of this function is to compute the tree edit distance.
	 * Based on the tree distance matrix the mapping is computed.
	 * 
	 * @return all pairs (ted1.node,ted2.node) of the minimal edit mapping. Each
	 *         element in the collection is an integer array A of size 2, where
	 *         A[0]=ted1.node is the postorderID (starting with 1) of the nodes
	 *         in ted1 and A[1]=ted2.node is the postorderID in ted2. The
	 *         postorderID of the empty node (insertion, deletion) is zero.
	 */
	public ArrayDeque<int[]> computeEditMapping() {

		// initialize tree and forest distance arrays
		double[][] treedist = new double[size1 + 1][size2 + 1];
		double[][] forestdist = new double[size1 + 1][size2 + 1];
		
		boolean rootNodePair = true;

		// treedist was already computed - the result is in delta and deltaBit
		for (int i = 0; i < size1; i++) {
			treedist[i][0] = i;
		}
		for (int j = 0; j < size2; j++) {
			treedist[0][j] = j;
		}
		for (int i = 1; i <= size1; i++) {
			for (int j = 1; j <= size2; j++) {
				treedist[i][j] = delta[i - 1][j - 1] + deltaBit[i - 1][j - 1];
			}
		}
		
		// forestdist for input trees has to be computed
		forestDist(it1, it2, size1, size2, treedist, forestdist);

		// empty edit mapping
		ArrayDeque<int[]> editMapping = new ArrayDeque<>();

		// empty stack of tree Pairs
		ArrayDeque<int[]> treePairs = new ArrayDeque<>();

		// push the pair of trees (ted1,ted2) to stack
		treePairs.addFirst(new int[] { size1, size2 });

		while (!treePairs.isEmpty()) {

			// get next tree pair to be processed
			int[] treePair = treePairs.removeFirst();
			int lastRow = treePair[0];
			int lastCol = treePair[1];

			// compute forest distance matrix
			if (!rootNodePair) {
				forestDist(it1, it2, lastRow, lastCol, treedist, forestdist);
			}
			rootNodePair = false;

			// compute mapping for current forest distance matrix
			int firstRow = it1.getInfo(POST2_LLD, lastRow - 1) + 1 - 1;
			int firstCol = it2.getInfo(POST2_LLD, lastCol - 1) + 1 - 1;
			int row = lastRow;
			int col = lastCol;
			while ((row > firstRow) || (col > firstCol)) {
				if ((row > firstRow)
						&& (forestdist[row - 1][col] + costDel == forestdist[row][col])) {
					// node with postorderID row is deleted from ted1
					editMapping.addFirst(new int[] { row, 0 });
					row--;
				} else if ((col > firstCol)
						&& (forestdist[row][col - 1] + costIns == forestdist[row][col])) {
					// node with postorderID col is inserted into ted2
					editMapping.addFirst(new int[] { 0, col });
					col--;
				} else {
					// node with postorderID row in ted1 is renamed to node col
					// in ted2

					if ((it1.getInfo(POST2_LLD, row - 1) == it1.getInfo(POST2_LLD, lastRow - 1))
							&& (it2.getInfo(POST2_LLD, col - 1) == it2.getInfo(POST2_LLD, lastCol - 1))) {
						// if both subforests are trees, map nodes
						editMapping.addFirst(new int[] { row, col });
						row--;
						col--;
					} else {
						// pop subtree pair
						treePairs.addFirst(new int[] { row, col });

						// continue with forest to the left of the popped
						// subtree pair
						row = it1.getInfo(POST2_LLD, row - 1) + 1 - 1;
						col = it2.getInfo(POST2_LLD, col - 1) + 1 - 1;
					}
				}
			}
		}
		return editMapping;
	}
	
	private void forestDist(InfoTree ted1, InfoTree ted2, int i, int j, double[][] treedist, double[][] forestdist) {
		forestdist[ted1.getInfo(POST2_LLD, i - 1) + 1 - 1][ted2.getInfo(POST2_LLD, j - 1) + 1 - 1] = 0;
		for (int di = ted1.getInfo(POST2_LLD, i - 1) + 1; di <= i; di++) {
			forestdist[di][ted2.getInfo(POST2_LLD, j - 1) + 1 - 1] = forestdist[di - 1][ted2.getInfo(POST2_LLD, j - 1) + 1 - 1] + costDel;
			for (int dj = ted2.getInfo(POST2_LLD, j - 1) + 1; dj <= j; dj++) {
				forestdist[ted1.getInfo(POST2_LLD, i - 1) + 1 - 1][dj] = forestdist[ted1.getInfo(POST2_LLD, i - 1) + 1 - 1][dj - 1]	+ costIns;

				if ((ted1.getInfo(POST2_LLD, di - 1) == ted1.getInfo(POST2_LLD, i - 1))
						&& (ted2.getInfo(POST2_LLD, dj - 1) == ted2.getInfo(POST2_LLD, j - 1))) {
					double costRen = 0;
					if (!(ted1.getInfo(POST2_LABEL, di - 1) == ted2.getInfo(POST2_LABEL, dj - 1))) {
						costRen = costMatch;
					}
					forestdist[di][dj] = Math.min(Math.min(
							forestdist[di - 1][dj] + costDel,
							forestdist[di][dj - 1] + costIns),
							forestdist[di - 1][dj - 1] + costRen);
					treedist[di][dj] = forestdist[di][dj];
				} else {
					forestdist[di][dj] = Math.min(Math.min(
							forestdist[di - 1][dj] + costDel,
							forestdist[di][dj - 1] + costIns),
							forestdist[ted1.getInfo(POST2_LLD, di - 1) + 1 - 1][ted2.getInfo(POST2_LLD, dj - 1) + 1 - 1]
									+ treedist[di][dj]);
				}
			}
		}
	}

}
