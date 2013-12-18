package fr.labri.gumtree.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Delete;
import fr.labri.gumtree.actions.model.Insert;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.actions.model.Update;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ActionGenerator {
	
	public final static Logger LOGGER = Logger.getLogger("fr.labri.gumtree.actions");
	
	private Tree origSrc;
	
	private Tree newSrc;

	private Tree origDst;
	
	private MappingStore origMappings;
	
	private MappingStore newMappings;
	
	private Set<Tree> dstInOrder;
	
	private Set<Tree> srcInOrder;
	
	private int lastId;
	
	private List<Action> actions;
	
	private TIntObjectMap<Tree> origSrcTrees;
	
	private TIntObjectMap<Tree> cpySrcTrees;
	
	public ActionGenerator(Tree src, Tree dst, MappingStore mappings) {
		this.origSrc = src;
		this.newSrc = this.origSrc.deepCopy();
		this.origDst = dst;
		
		origSrcTrees = new TIntObjectHashMap<Tree>();
		for (Tree t: origSrc.getTrees()) origSrcTrees.put(t.getId(), t);
		cpySrcTrees = new TIntObjectHashMap<Tree>();
		for (Tree t: newSrc.getTrees()) cpySrcTrees.put(t.getId(), t);
		
		origMappings = new MappingStore();
		for (Mapping m: mappings) this.origMappings.link(cpySrcTrees.get(m.getFirst().getId()), m.getSecond());
		this.newMappings = origMappings.copy();
	}
	
	public List<Action> getActions() {
		return actions;
	}
	
	public void generate() {
		Tree vsrc = new Tree(-1, "");
		Tree vdst = new Tree(-1, "");
		vsrc.setId(-1);
		vdst.setId(-1);
		vsrc.addChild(newSrc);
		vdst.addChild(origDst);
		
		actions = new ArrayList<Action>();
		dstInOrder = new HashSet<Tree>();
		srcInOrder = new HashSet<Tree>();
		
		lastId = newSrc.getSize() + 1;
		newMappings.link(vsrc, vdst);
		
		List<Tree> bfsDst = TreeUtils.bfsOrder(origDst); 
		for (Tree x: bfsDst) {
			Tree w = null;
			Tree y = x.getParent();
			Tree z = newMappings.getSrc(y);
			
			if (!newMappings.hasDst(x)) {
				int k = findPos(x);
				// Insertion case : insert new node.
				int wId = newId();
				w = new Tree(x.getType(), x.getLabel());
				w.setTypeLabel(x.getTypeLabel());
				w.setPos(x.getPos());
				w.setLength(x.getLength());
				w.setId(wId);
				// In order to use the real nodes from the second tree, we
				// furnish x instead of w and fake that x has the newly
				// generated ID.
				Action ins = new Insert(x, origSrcTrees.get(z.getId()), k);
				actions.add(ins);
				//System.out.println(ins);
				origSrcTrees.put(w.getId(), x);
				newMappings.link(w, x);
				z.getChildren().add(k, w);
				w.setParent(z);
			} else {
				w = newMappings.getSrc(x);
				if (!x.equals(origDst)) {
					Tree v = w.getParent();
					if (!w.getLabel().equals(x.getLabel())) {
						actions.add(new Update(origSrcTrees.get(w.getId()), x.getLabel()));
						w.setLabel(x.getLabel());
					}
					if (!newMappings.getSrc(y).equals(v)) {
						int k = findPos(x);
						Action mv = new Move(origSrcTrees.get(w.getId()), origSrcTrees.get(z.getId()), k);
						actions.add(mv);
						//System.out.println(mv);
						w.getParent().getChildren().remove(w);
						z.getChildren().add(k, w);
						w.setParent(z);
					}
				}			
			}
			
			//FIXME not sure why :D
			srcInOrder.add(w);
			dstInOrder.add(x);
			alignChildren(w, x);
		}
		
		List<Tree> poSrc = TreeUtils.postOrder(newSrc);	
		for (Tree w : poSrc) {
			if (!newMappings.hasSrc(w)) {
				actions.add(new Delete(origSrcTrees.get(w.getId())));
				w.getParent().getChildren().remove(w);
			}
		}
		
		if (!newSrc.toDigestTreeString().equals(origDst.toDigestTreeString())) {
			LOGGER.severe("Trees not isomorphics!");
			newSrc.refresh();
			//System.out.println(newSrc.toTreeString());
			//System.out.println(origDst.toTreeString());
		}
	}
	
	private void alignChildren(Tree w, Tree x) {
		srcInOrder.removeAll(w.getChildren());
		dstInOrder.removeAll(x.getChildren());
		
		List<Tree> s1 = new ArrayList<Tree>();
		for (Tree c: w.getChildren()) 
			if (newMappings.hasSrc(c)) 
				if (x.getChildren().contains(newMappings.getDst(c))) 
					s1.add(c);
		
		List<Tree> s2 = new ArrayList<Tree>();
		for (Tree c: x.getChildren()) 
			if (newMappings.hasDst(c)) 
				if (w.getChildren().contains(newMappings.getSrc(c))) 
					s2.add(c);

		List<Mapping> lcs = lcs(s1, s2);
		
		for (Mapping m : lcs) {
			srcInOrder.add(m.getFirst());
			dstInOrder.add(m.getSecond());
		}
		
		for (Tree a : s1) {
			for (Tree b: s2 ) {
				if (origMappings.has(a, b)) {
					if (!lcs.contains(new Mapping(a, b))) {
						int k = findPos(b);
						Action mv = new Move(origSrcTrees.get(a.getId()), origSrcTrees.get(w.getId()), k);
						actions.add(mv);
						//System.out.println(mv);
						a.getParent().getChildren().remove(a);
						w.getChildren().add(k, a);
						a.setParent(w);
						srcInOrder.add(a);
				 		dstInOrder.add(b);
					}
				}
			}
		}
	}
	
	private int findPos(Tree x) {
		Tree y = x.getParent();
		List<Tree> siblings = y.getChildren();
		
		for (Tree c : siblings) {
			if (dstInOrder.contains(c)) {
				if (c.equals(x)) return 0;
				else break;		
			}
		}
		
		int xpos = siblings.indexOf(x);
		Tree v = null;
		for (int i = 0; i < xpos; i++) {
			Tree c = siblings.get(i);
			if (dstInOrder.contains(c)) v = c;
		}
		
		//if (v == null) throw new RuntimeException("No rightmost sibling in order");
		if (v == null) return 0;
		
		Tree u = newMappings.getSrc(v);
		siblings = u.getParent().getChildren();
		int upos = siblings.indexOf(u);
		int r = 0;
		for (int i = 0; i <= upos; i++) if (srcInOrder.contains(siblings.get(i))) r++;
		
		return r;
	}
	
	private int newId() {
		return ++lastId;
	}
	
    private List<Mapping> lcs(List<Tree> x, List<Tree> y) {
        int m = x.size();
        int n = y.size();
        List<Mapping> lcs = new ArrayList<Mapping>();
        
        int[][] opt = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (newMappings.getSrc(y.get(j)).equals(x.get(i))) opt[i][j] = opt[i + 1][j + 1] + 1;
                else  opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }
     
        int i = 0, j = 0;
        while (i < m && j < n) {
            if (newMappings.getSrc(y.get(j)).equals(x.get(i))) {
            	lcs.add(new Mapping(x.get(i), y.get(j)));
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) i++;
            else j++;
        }
       
        return lcs;
    }
	
}
