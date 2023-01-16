package ai.serenade.treesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class ParserTest extends TestBase {
  @Test
  void testParse() throws UnsupportedEncodingException {
    try (Parser parser = new Parser()) {
      parser.setLanguage(Languages.java());
      try (Tree tree = parser.parseString("public class Thing { public static void main() { System.out.println('a'); } }")) {
        assertEquals(
                "(program (class_declaration (modifiers) name: (identifier) body: (class_body (method_declaration (modifiers) type: (void_type) name: (identifier) parameters: (formal_parameters) body: (block (expression_statement (method_invocation object: (field_access object: (identifier) field: (identifier)) name: (identifier) arguments: (argument_list (character_literal)))))))))",
                tree.getRootNode().getNodeString()
        );
      }
    }
  }

  private void depthFirstWalk(Node rootNode) {
    try (var treeCursor = rootNode.walk()) {
      while (treeCursor.hasNext()) {
        var currentNode = treeCursor.next();
        var range = currentNode.getRange();
        var format = "Type: %s, Range: %s";
        var string = String.format(format, currentNode.getType(), range);
        System.out.println(string);
      }
    }
  }

  @Test
  void regenerateSource() throws Exception {
    try (var parser = new Parser()) {
      parser.setLanguage(Languages.java());
      var path = Paths.get("./src/main/java/ai/serenade/treesitter/TestFile.java");
      String program = Files.readString(path);
      try (Tree tree = parser.parseString(program)) {
        depthFirstWalk(tree.getRootNode());
      }
    }
  }

  private String sitterTreeToString(Tree tree) {
    String format = "Type: %s, Range: %s\n";
    var root = tree.getRootNode();
    var builder = new StringBuilder(String.format(format, root.getType(), root.getRange()));
    try (var cursor = root.walk()) {
      while (cursor.hasNext()) {
        var current = cursor.next().getNode();
        String string = String.format(format, current.getType(), current.getRange());
        builder.append(string);
      }
    }

    // For some reason, the program node is appended again at the end.
    // FIXME: Remove that.
    return builder.toString();
  }

  @Test
  void castExpressionAutofixTest() throws Exception {
    try (var parser = new Parser()) {
      parser.setLanguage(Languages.java());
      String srcStr = "./src/test/java/ai/serenade/treesitter/TestFile.java";
      var path = Paths.get(srcStr);
      String program = Files.readString(path);
      try (Tree tree = parser.parseString(program)) {
        TreeWrapper myTree = new TreeWrapper(tree);
        Span position = new Span(new Range(7, 24, 7, 52));
        var offendingCastExpression = myTree.nodeAtSpan(position);
        for (var child : offendingCastExpression.children()) {
          if (child.getInternalNode().getType().equals("(")) {
            child.setDeleted(true);
          }

          if (child.getInternalNode().getType().equals(")")) {
            child.setDeleted(true);
          }

          if (child.getInternalNode().getType().equals("generic_type")) {
            child.setDeleted(true);
          }
        }

        System.out.println(myTree.generateSource(srcStr));
      }
    }
  }
}
