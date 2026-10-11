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

    @GetMapping("/keep-business-receipt")
    public String keepBusinessReceipt() {
        return "tool/keep-business-receipt/index";
    }
}
