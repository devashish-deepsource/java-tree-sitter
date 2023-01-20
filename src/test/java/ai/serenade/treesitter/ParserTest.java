package ai.serenade.treesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import mytree.MergeNode;
import mytree.Span;
import mytree.TreeWrapper;
import org.junit.jupiter.api.Test;
import srcgen.Action;
import srcgen.InsertSibling;

import javax.lang.model.element.Modifier;

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
                TreeWrapper myTree = new TreeWrapper(tree, Files.readString(path));
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

                System.out.println(myTree.generateSource());
            }
        }
    }

    private String generateStatement(String statement) {
        var klass = TypeSpec.classBuilder("Temp").addModifiers(Modifier.PUBLIC);
        var method = MethodSpec.methodBuilder("test")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement(statement)
                .build();
        klass.addMethod(method);
        return klass.build().toString();
    }

    private Tree generatePartialTree(Parser parser, String programString) throws Exception {
        parser.setLanguage(Languages.java());
        return parser.parseString(programString);
    }

    @Test
    void insertStatementAboveAndBelow() throws Exception {
        try (var parser = new Parser()) {
            parser.setLanguage(Languages.java());
            String srcStr = "./src/test/java/ai/serenade/treesitter/TestFile.java";
            var path = Paths.get(srcStr);
            String program = Files.readString(path);
            try (Tree tree = parser.parseString(program)) {
                TreeWrapper mainTree = new TreeWrapper(tree, Files.readString(path));
                var soutSpan = new Span(new Range(8, 12, 8, 41));
                var soutNodeParent = mainTree.nodeAtSpan(soutSpan).parent();

                // We want to create a valid parse tree even for fragments of code like this: `callMe()`.
                // So we'll always generate some boilerplate code so that the output is a valid Java program.
                // For example, here we are wrapping the call statement in a method which in-turn is defined in a class called `Temp`.
                String callStmtString = generateStatement("callMe()");
                var partialTree = generatePartialTree(parser, callStmtString);
                TreeWrapper myPartialTree = new TreeWrapper(partialTree, callStmtString);
                // FIXME: We are hardcoding the span of the node that we want to insert into the main tree.
                //  Ideally, we'll use tree-sitter queries for this.
                var callNodeSpan = new Span(new Range(2, 4, 2, 13));
                var callStmtNodePrepend = new MergeNode(myPartialTree.nodeAtSpan(callNodeSpan), soutNodeParent, callStmtString, true, soutSpan);
                Action actInsertBefore = new InsertSibling(mainTree, soutSpan, callStmtNodePrepend, true);
                actInsertBefore.apply();

                var callStmtNodeAppend = new MergeNode(myPartialTree.nodeAtSpan(callNodeSpan), soutNodeParent, callStmtString, true, soutSpan);
                Action actInsertAfter = new InsertSibling(mainTree, soutSpan, callStmtNodeAppend, false);
                actInsertAfter.apply();

                var source = mainTree.generateSource();
                System.out.println(source);
            }
        }
    }

    private void removeCastExpression(TreeWrapper mainTree) {
        Span position = new Span(new Range(7, 24, 7, 52));
        var offendingCastExpression = mainTree.nodeAtSpan(position);
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
    }

    private void insertCalls(Parser parser, TreeWrapper mainTree) throws Exception {
        var soutSpan = new Span(new Range(8, 12, 8, 41));
        var soutNodeParent = mainTree.nodeAtSpan(soutSpan).parent();

        String callStmtString = generateStatement("callMe()");
        var partialTree = generatePartialTree(parser, callStmtString);
        TreeWrapper myPartialTree = new TreeWrapper(partialTree, callStmtString);
        var callNodeSpan = new Span(new Range(2, 4, 2, 13));
        var callStmtNodePrepend = new MergeNode(myPartialTree.nodeAtSpan(callNodeSpan), soutNodeParent, callStmtString, true, soutSpan);
        Action actInsertBefore = new InsertSibling(mainTree, soutSpan, callStmtNodePrepend, true);
        actInsertBefore.apply();

        var callStmtNodeAppend = new MergeNode(myPartialTree.nodeAtSpan(callNodeSpan), soutNodeParent, callStmtString, true, soutSpan);
        Action actInsertAfter = new InsertSibling(mainTree, soutSpan, callStmtNodeAppend, false);
        actInsertAfter.apply();
    }

    @Test
    public void fixMultiple() throws Exception {
        try (var parser = new Parser()) {
            parser.setLanguage(Languages.java());
            String srcStr = "./src/test/java/ai/serenade/treesitter/TestFile.java";
            var path = Paths.get(srcStr);
            String program = Files.readString(path);
            var tree = parser.parseString(program);
            var mainTree = new TreeWrapper(tree, program);
            removeCastExpression(mainTree);
            insertCalls(parser, mainTree);
            System.out.println(mainTree.generateSource());
        }
    }
}
