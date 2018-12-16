package com.vladsch.flexmark.samples;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.CustomNode;
import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.internal.Delimiter;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor;
import com.vladsch.flexmark.parser.delimiter.DelimiterRun;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A sample that demonstrates how to add HTML rendering support for a custom token, in this case,
 * {@code '+'} which is HTML rendered using the {@code ins} tag.
 */
public class UnderlineExtensionSample {

    static final MutableDataSet OPTIONS = new MutableDataSet();
    static {
        OPTIONS.set(Parser.EXTENSIONS, Arrays.asList(UnderlineExtension.create()));
    }

    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    static class Underline extends CustomNode implements DelimitedNode {

        private BasedSequence openingMarker = BasedSequence.NULL;
        private BasedSequence text = BasedSequence.NULL;
        private BasedSequence closingMarker = BasedSequence.NULL;

        @Override
        public BasedSequence[] getSegments() {
            return new BasedSequence[] {openingMarker, text, closingMarker };
        }

        @Override
        public void getAstExtra(StringBuilder out) {
            Node.delimitedSegmentSpan(out, openingMarker, text, closingMarker, "text");
        }

        public Underline() { }

        public Underline(BasedSequence chars) {
            super(chars);
        }

        public Underline(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
            super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));
            this.openingMarker = openingMarker;
            this.text = text;
            this.closingMarker = closingMarker;
        }

        public BasedSequence getOpeningMarker() {
            return openingMarker;
        }

        public void setOpeningMarker(BasedSequence openingMarker) {
            this.openingMarker = openingMarker;
        }

        public BasedSequence getText() {
            return text;
        }

        public void setText(BasedSequence text) {
            this.text = text;
        }

        public BasedSequence getClosingMarker() {
            return closingMarker;
        }

        public void setClosingMarker(BasedSequence closingMarker) {
            this.closingMarker = closingMarker;
        }

    }

    static class UnderlineDelimiterProcessor implements DelimiterProcessor {

        @Override
        public char getOpeningCharacter() {
            return '+';
        }

        @Override
        public char getClosingCharacter() {
            return '+';
        }

        @Override
        public int getMinLength() {
            return 1;
        }

        @Override
        public int getDelimiterUse(DelimiterRun opener, DelimiterRun closer) {
            if (opener.length() >= 1 && closer.length() >= 1) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean canBeOpener(String before, String after, boolean leftFlanking, boolean rightFlanking, boolean beforeIsPunctuation, boolean afterIsPunctuation, boolean beforeIsWhitespace, boolean afterIsWhiteSpace) {
            return leftFlanking;
        }

        @Override
        public boolean canBeCloser(String before, String after, boolean leftFlanking, boolean rightFlanking, boolean beforeIsPunctuation, boolean afterIsPunctuation, boolean beforeIsWhitespace, boolean afterIsWhiteSpace) {
            return rightFlanking;
        }

        @Override
        public Node unmatchedDelimiterNode(InlineParser inlineParser, DelimiterRun delimiter) {
            return null;
        }

        @Override
        public boolean skipNonOpenerCloser() {
            return false;
        }

        @Override
        public void process(Delimiter opener, Delimiter closer, int delimitersUsed) {
            Underline underline = new Underline(opener.getTailChars(delimitersUsed), BasedSequence.NULL,
                    closer.getLeadChars(delimitersUsed));
            opener.moveNodesBetweenDelimitersTo(underline, closer);
        }

    }

    static class UnderlineNodeRenderer implements NodeRenderer {

        public static class Factory implements NodeRendererFactory {

            @Override
            public NodeRenderer create(DataHolder options) {
                return new UnderlineNodeRenderer();
            }

        }

        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
            set.add(new NodeRenderingHandler<>(Underline.class, new CustomNodeRenderer<Underline>() {
                @Override
                public void render(Underline node, NodeRendererContext context, HtmlWriter html) {
                    if (context.getHtmlOptions().sourcePositionParagraphLines) {
                        html.withAttr().tag("ins");
                    } else {
                        html.srcPos(node.getText()).withAttr().tag("ins");
                    }
                    context.renderChildren(node);
                    html.tag("/ins");
                }
            }));

            return set;
        }

    }

    /**
     * A custom Flexmark Java extension to support the usage of {@code '+'} as a Markdown syntax
     * element/token for rendering underlined text with the HTML {@code ins} tag.
     */
    static class UnderlineExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

        private UnderlineExtension() { }

        @Override
        public void rendererOptions(MutableDataHolder options) { }

        @Override
        public void parserOptions(MutableDataHolder options) { }

        @Override
        public void extend(Parser.Builder parserBuilder) {
            parserBuilder.customDelimiterProcessor(new UnderlineDelimiterProcessor());
        }

        @Override
        public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
            rendererBuilder.nodeRendererFactory(new UnderlineNodeRenderer.Factory());
        }

        public static Extension create() {
            return new UnderlineExtension();
        }

    }

    public static void main(String[] args) {
        final String original = "**bold** _italic_ +underline+ *+bold and underline+*";

        Node document = PARSER.parse(original);
        String html = RENDERER.render(document);

        // <p><strong>bold</strong> <em>italic</em> <ins>underline</ins> <em><ins>bold and underline</ins></em></p>
        System.out.println(html);
    }

}
