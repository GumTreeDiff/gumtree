import fr.labri.gumtree.gen.antlrcss.CssGrammarTreeGenerator;
import fr.labri.gumtree.tree.TreeContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

public class BasicTestCss {

    @Test
    public void testParse() throws IOException {
        CssGrammarTreeGenerator gen = new CssGrammarTreeGenerator();
        TreeContext res = gen.generate(new InputStreamReader(getClass().getResourceAsStream("trivial.css")));
        System.out.println(res);
    }
}
