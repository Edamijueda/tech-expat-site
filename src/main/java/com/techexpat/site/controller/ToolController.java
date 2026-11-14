package com.techexpat.site.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tool")
public class ToolController {

    @GetMapping
    public String index() {
        return "tool/index";
    }

    @GetMapping("/keep-business-docs")
    public String keepBusinessDocs() {
        return "tool/keep-business-docs/index";
    }
}
