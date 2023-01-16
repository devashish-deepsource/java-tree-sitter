package ai.serenade.treesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.Test;

public class NodeTest extends TestBase {

    @Test
    void testGetChildren() throws UnsupportedEncodingException {
        try (Parser parser = new Parser()) {
            parser.setLanguage(Languages.java());
            try (Tree tree = parser.parseString("public class Thing extends Object { public static void main() { System.out.println('a'); } }")) {

                Node root = tree.getRootNode();
                assertEquals(1, root.getChildCount());
                assertEquals("program", root.getType());
                assertEquals(0, root.getStartByte());
                assertEquals(92, root.getEndByte());

                Node classDecl = root.getChild(0);
                assertEquals("class_declaration", classDecl.getType());
                assertEquals("superclass", classDecl.getChildByFieldName("superclass").getType());
                assertEquals(4, classDecl.getChildCount());
            }
        }
    }

    @Test
    void testErrors() throws UnsupportedEncodingException {
      try (Parser parser = new Parser()) {
        parser.setLanguage(Languages.java());
        try (Tree tree = parser.parseString("class a { int; }")) {
          Node root = tree.getRootNode();
          assert(root.hasError());
        }
      }
    }
}
