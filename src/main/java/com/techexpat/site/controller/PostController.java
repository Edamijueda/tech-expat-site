package com.techexpat.site.controller;

import com.techexpat.site.model.Post;
import com.techexpat.site.model.Section;
import com.techexpat.site.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PostController {

    private static final String SECTION_PATTERN = "research|blog|tutorial";

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{section:" + SECTION_PATTERN + "}")
    public String index(@PathVariable String section, Model model) {
        Section s = Section.fromSlug(section);
        model.addAttribute("section", s);
        model.addAttribute("posts", postService.findBySection(s));
        return "posts/index";
    }

    @GetMapping("/{section:" + SECTION_PATTERN + "}/{slug}")
    public String post(@PathVariable String section, @PathVariable String slug, Model model) {
        Section s = Section.fromSlug(section);
        Post post = postService.findBySectionAndSlug(s, slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("section", s);
        model.addAttribute("post", post);
        return "posts/post";
    }
}
