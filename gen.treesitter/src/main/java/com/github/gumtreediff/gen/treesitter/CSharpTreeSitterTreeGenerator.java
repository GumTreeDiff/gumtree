package com.github.gumtreediff.gen.treesitter;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;

@Register(id = "cs-treesitter", accept = "\\.[cs]$", priority = Registry.Priority.HIGH)
public class CSharpTreeSitterTreeGenerator extends AbstractTreeSitterGenerator {
    private static final String CSHARP_PARSER_NAME = "csharp";

    @Override
    public String getParserName() {
        return CSHARP_PARSER_NAME;
    }
}