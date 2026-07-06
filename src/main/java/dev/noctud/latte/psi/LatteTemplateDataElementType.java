package dev.noctud.latte.psi;

import com.intellij.lang.Language;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class LatteTemplateDataElementType extends TemplateDataElementType {

    private final TokenSet elementTypesSet;
    private boolean potentiallyWorkingWithAttribute;
    private boolean lookForTagClose;

    public LatteTemplateDataElementType(@NonNls String debugName,
                                        Language language,
                                        @NotNull TokenSet htmlTemplateElementType,
                                        @NotNull IElementType outerElementType) {
        super(debugName, language, htmlTemplateElementType.getTypes()[0], outerElementType);
        elementTypesSet = htmlTemplateElementType;
    }

    protected CharSequence createTemplateText(@NotNull CharSequence sourceCode,
                                              @NotNull Lexer baseLexer,
                                              @NotNull RangeCollector rangeCollector) {
        StringBuilder result = new StringBuilder(sourceCode.length());
        baseLexer.start(sourceCode);

        TextRange currentRange = TextRange.EMPTY_RANGE;
        while (baseLexer.getTokenType() != null) {
            TextRange newRange = TextRange.create(baseLexer.getTokenStart(), baseLexer.getTokenEnd());
            assert currentRange.getEndOffset() == newRange.getStartOffset() :
                "Inconsistent tokens stream from " + baseLexer +
                    ": " + getRangeDump(currentRange, sourceCode) + " followed by " + getRangeDump(newRange, sourceCode);
            currentRange = newRange;
            if (potentiallyWorkingWithAttribute && baseLexer.getTokenType() == LatteTypes.T_MACRO_OPEN_TAG_OPEN) {
                potentiallyWorkingWithAttribute = false;
                result.append("\"");
                appendCurrentTemplateToken(baseLexer.getTokenEnd(), sourceCode);
                lookForTagClose = true;
            } else if (lookForTagClose && baseLexer.getTokenType() == LatteTypes.T_MACRO_TAG_CLOSE) {
                result.append("\"");
                appendCurrentTemplateToken(baseLexer.getTokenEnd(), sourceCode);
                lookForTagClose = false;
            } else if (elementTypesSet.contains(baseLexer.getTokenType())) {
                potentiallyWorkingWithAttribute = baseLexer.getTokenType() == LatteTypes.T_TEXT && sourceCode.charAt(baseLexer.getTokenEnd()) == '=' && !lookForTagClose;
                result.append(sourceCode, baseLexer.getTokenStart(), baseLexer.getTokenEnd());
                appendCurrentTemplateToken(baseLexer.getTokenEnd(), sourceCode);
            } else {
                potentiallyWorkingWithAttribute = false;
                rangeCollector.addOuterRange(currentRange);
            }
            baseLexer.advance();
        }

        return result;
    }

    @NotNull
    private static String getRangeDump(@NotNull TextRange range, @NotNull CharSequence sequence) {
        return "'" + StringUtil.escapeLineBreak(range.subSequence(sequence).toString()) + "' " + range;
    }

}
