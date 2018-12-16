package com.vladsch.flexmark.samples;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory;
import com.vladsch.flexmark.util.NodeTracker;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.Arrays;

/**
 * A sample that demonstrates how to strip (replace) specific tokens from a parsed
 * {@link Document} prior to rendering.
 */
public class TokenReplacingPostProcessorSample {

    static final MutableDataSet OPTIONS = new MutableDataSet();
    static {
        OPTIONS.set(Parser.EXTENSIONS, Arrays.asList(LinkReplacingExtension.create()));
    }

    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    static class LinkReplacingPostProcessor extends NodePostProcessor {

        static class Factory extends NodePostProcessorFactory {

            public Factory(DataHolder options) {
                super(false);

                addNodes(Link.class);
                addNodes(Image.class);
            }

            @Override
            public NodePostProcessor create(Document document) {
                return new LinkReplacingPostProcessor();
            }

        }

        @Override
        public void process(NodeTracker state, Node node) {
            if (node instanceof Link) { // [foo](http://example.com)
                Link link = (Link) node;
                Text text = new Text(link.getText());
                link.insertAfter(text);
                state.nodeAdded(text);

                link.unlink();
                state.nodeRemoved(link);
            } else if (node instanceof Image) { // ![bar](http://example.com)
                Image image = (Image) node;
                Text text = new Text(image.getText());
                image.insertAfter(text);
                state.nodeAdded(text);

                image.unlink();
                state.nodeRemoved(image);
            }
        }

    }

    /**
     * An extension that registers a post processor which intentionally strips (replaces)
     * specific link and image-link tokens after parsing.
     */
    static class LinkReplacingExtension implements Parser.ParserExtension {

        private LinkReplacingExtension() { }

        @Override
        public void parserOptions(MutableDataHolder options) { }

        @Override
        public void extend(Parser.Builder parserBuilder) {
            parserBuilder.postProcessorFactory(new LinkReplacingPostProcessor.Factory(parserBuilder));
        }

        public static Extension create() {
            return new LinkReplacingExtension();
        }

    }

    public static void main(String[] args) {
        final String original = "[foo](http://example.com) ![bar](http://example.com) **baz**";

        Node document = PARSER.parse(original);
        String html = RENDERER.render(document);

        // <p>foo bar <strong>baz</strong></p>
        System.out.println(html);
    }

}
