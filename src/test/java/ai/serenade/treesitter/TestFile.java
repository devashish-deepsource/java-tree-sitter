package ai.serenade.treesitter;

import java.util.List;

public class TestFile {
    public void test() {
        // The cast to List<String> is not required. Let's remove it using autofix.
        for (String s : (List<String>) manyStrings()) {
            System.out.println("Middle");
        }
    }

    // A hello world program.
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }

    public List<String> manyStrings() {
        return List.of("a", "b");
    }
}
