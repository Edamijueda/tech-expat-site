package com.techexpat.site.controller;

import com.techexpat.site.model.ResearchPost;
import com.techexpat.site.service.ResearchService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ResearchController {

    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) {
        this.researchService = researchService;
    }

    @GetMapping("/research")
    public String index(Model model) {
        model.addAttribute("posts", researchService.findAll());
        return "research/index";
    }

    @GetMapping("/research/{slug}")
    public String post(@PathVariable String slug, Model model) {
        ResearchPost post = researchService.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("post", post);
        return "research/post";
    }
}
