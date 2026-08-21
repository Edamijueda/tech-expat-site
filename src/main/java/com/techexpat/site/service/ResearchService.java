package com.techexpat.site.service;

import com.techexpat.site.model.ResearchPost;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResearchService {

    private static final String RESOURCE_PATTERN = "classpath:/research/*.md";
    private static final int WORDS_PER_MINUTE = 220;
    private static final List<org.commonmark.Extension> EXTENSIONS =
            List.of(YamlFrontMatterExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private final List<ResearchPost> posts;
    private final Map<String, ResearchPost> bySlug;

    public ResearchService() throws IOException {
        List<ResearchPost> loaded = loadAll(new PathMatchingResourcePatternResolver());
        this.posts = List.copyOf(loaded);
        this.bySlug = loaded.stream()
                .collect(Collectors.toUnmodifiableMap(ResearchPost::slug, p -> p));
    }

    public List<ResearchPost> findAll() {
        return posts;
    }

    public Optional<ResearchPost> findBySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    private static List<ResearchPost> loadAll(PathMatchingResourcePatternResolver resolver) throws IOException {
        List<ResearchPost> loaded = new ArrayList<>();
        for (Resource resource : resolver.getResources(RESOURCE_PATTERN)) {
            try (var in = resource.getInputStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                loaded.add(parse(content));
            }
        }
        loaded.sort(Comparator.comparingInt(ResearchPost::order));
        return loaded;
    }

    static ResearchPost parse(String markdown) {
        Node document = PARSER.parse(markdown);

        YamlFrontMatterVisitor frontMatter = new YamlFrontMatterVisitor();
        document.accept(frontMatter);
        Map<String, List<String>> data = frontMatter.getData();

        String slug = require(data, "slug");
        int order = Integer.parseInt(require(data, "order"));
        String title = require(data, "title");
        String author = require(data, "author");
        LocalDate date = LocalDate.parse(require(data, "date"));
        String description = require(data, "description");

        String htmlBody = RENDERER.render(document);
        int readingMinutes = computeReadingMinutes(document);

        return new ResearchPost(slug, order, title, author, date, description, htmlBody, readingMinutes);
    }

    private static String require(Map<String, List<String>> data, String key) {
        List<String> values = data.get(key);
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Missing front matter key: " + key);
        }
        return values.get(0);
    }

    private static int computeReadingMinutes(Node document) {
        WordCountVisitor visitor = new WordCountVisitor();
        document.accept(visitor);
        return Math.max(1, visitor.wordCount / WORDS_PER_MINUTE);
    }

    private static class WordCountVisitor extends AbstractVisitor {
        int wordCount = 0;

        @Override
        public void visit(Text text) {
            String literal = text.getLiteral();
            if (literal != null && !literal.isBlank()) {
                wordCount += literal.trim().split("\\s+").length;
            }
        }

        @Override
        public void visit(CustomBlock block) {
        }

        @Override
        public void visit(CustomNode node) {
        }
    }
}
