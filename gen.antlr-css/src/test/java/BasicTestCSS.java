import fr.labri.gumtree.gen.antlrcss.CSSGrammarTreeGenerator;
import fr.labri.gumtree.tree.TreeContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

public class BasicTestCSS {

    @Test
    public void testParse() throws IOException {
        CSSGrammarTreeGenerator gen = new CSSGrammarTreeGenerator();
        TreeContext res = gen.generate(new InputStreamReader(getClass().getResourceAsStream("trivial.css")));
        System.out.println(res);
    }
}
