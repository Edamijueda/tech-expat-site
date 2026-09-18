package com.techexpat.site.model;

public enum Section {
    RESEARCH("research", "Research"),
    BLOG("blog", "Blog"),
    TUTORIAL("tutorial", "Tutorial");

    private final String slug;
    private final String label;

    Section(String slug, String label) {
        this.slug = slug;
        this.label = label;
    }

    public String slug() {
        return slug;
    }

    public String label() {
        return label;
    }

    public static Section fromSlug(String slug) {
        for (Section section : values()) {
            if (section.slug.equals(slug)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown section: " + slug);
    }
}
