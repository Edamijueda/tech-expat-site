package com.techexpat.site.service;

import com.techexpat.site.model.Post;
import com.techexpat.site.model.Section;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final String RESOURCE_PATTERN = "classpath:/posts/*.md";
    private static final int WORDS_PER_MINUTE = 220;
    private static final List<org.commonmark.Extension> EXTENSIONS =
            List.of(YamlFrontMatterExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private final List<Post> posts;
    private final Map<String, Post> bySlug;

    public PostService() throws IOException {
        List<Post> loaded = loadAll(new PathMatchingResourcePatternResolver());
        this.posts = List.copyOf(loaded);
        this.bySlug = loaded.stream()
                .collect(Collectors.toUnmodifiableMap(Post::slug, p -> p));
    }

    public List<Post> findAll() {
        return posts;
    }

    public List<Post> findBySection(Section section) {
        return posts.stream()
                .filter(p -> p.sections().contains(section))
                .toList();
    }

    public Optional<Post> findBySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    public Optional<Post> findBySectionAndSlug(Section section, String slug) {
        return findBySlug(slug).filter(p -> p.sections().contains(section));
    }

    private static List<Post> loadAll(PathMatchingResourcePatternResolver resolver) throws IOException {
        List<Post> loaded = new ArrayList<>();
        for (Resource resource : resolver.getResources(RESOURCE_PATTERN)) {
            try (var in = resource.getInputStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                loaded.add(parse(content));
            }
        }
        loaded.sort(Comparator.comparingInt(Post::order));
        return loaded;
    }

    static Post parse(String markdown) {
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
        int aiPercent = requireAiPercent(data, slug);
        LocalDate updatedDate = optional(data, "updated").map(LocalDate::parse).orElse(null);
        Set<Section> sections = requireSections(data, slug);

        String htmlBody = RENDERER.render(document);
        int readingMinutes = computeReadingMinutes(document);

        return new Post(slug, order, title, author, date, description, htmlBody, readingMinutes, aiPercent, updatedDate, sections);
    }

    private static int requireAiPercent(Map<String, List<String>> data, String slug) {
        int value = Integer.parseInt(require(data, "ai_percent"));
        if (value < 0 || value > 100) {
            throw new IllegalStateException(
                    "ai_percent must be between 0 and 100 for post '" + slug + "', got " + value);
        }
        return value;
    }

    private static Set<Section> requireSections(Map<String, List<String>> data, String slug) {
        List<String> values = data.get("sections");
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Missing or empty 'sections' for post '" + slug + "'");
        }
        EnumSet<Section> sections = EnumSet.noneOf(Section.class);
        for (String value : values) {
            try {
                sections.add(Section.fromSlug(value.trim()));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Unknown section '" + value + "' in post '" + slug + "'", e);
            }
        }
        return Set.copyOf(sections);
    }

    private static Optional<String> optional(Map<String, List<String>> data, String key) {
        List<String> values = data.get(key);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        String value = values.get(0);
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
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
