package com.nova.keyboard.data

/** Editable starter snippets. Every one can be changed or deleted by the user. */
object DefaultSnippets {
    val all: List<Snippet> = listOf(
        Snippet(
            0, "HTML document", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"utf-8\" />\n  <title>Title</title>\n</head>\n<body>\n" +
                "  <h1>Hello</h1>\n</body>\n</html>", "html", true
        ),
        Snippet(
            0, "CSS reset", "*, *::before, *::after { box-sizing: border-box; }\n" +
                "body { margin: 0; font-family: system-ui, sans-serif; }", "css", false
        ),
        Snippet(
            0, "JavaScript function", "function name(args) {\n  return args;\n}", "javascript", false
        ),
        Snippet(
            0, "Python function", "def name(args):\n    return args\n", "python", false
        ),
        Snippet(
            0, "JSON object", "{\n  \"key\": \"value\"\n}", "json", false
        ),
        Snippet(
            0, "SQL select", "SELECT column FROM table WHERE condition ORDER BY column DESC LIMIT 10;", "sql", false
        ),
        Snippet(
            0, "PHP function", "<?php\nfunction name(\$args) {\n    return \$args;\n}\n?>", "php", false
        ),
        Snippet(
            0, "Kotlin class", "class Example(private val value: String) {\n    fun run() = value\n}", "kotlin", false
        ),
        Snippet(
            0, "TypeScript interface", "interface User {\n  id: string;\n  name: string;\n}", "typescript", false
        ),
        Snippet(
            0, "Java class", "public class Example {\n    private final String value;\n" +
                "    public Example(String value) { this.value = value; }\n}", "java", false
        ),
        Snippet(
            0, "C++ main", "#include <iostream>\nint main() {\n    std::cout << \"Hello\";\n    return 0;\n}", "cpp", false
        ),
        Snippet(
            0, "Bash loop", "for f in *.txt; do\n  echo \"\$f\"\ndone", "bash", false
        )
    )
}
