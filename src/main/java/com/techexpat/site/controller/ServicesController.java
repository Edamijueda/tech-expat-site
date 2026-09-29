package com.techexpat.site.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServicesController {

    private static final String VIEW = "services/coming-soon";

    @GetMapping("/it-consulting")
    public String itConsulting(Model model) {
        return page(model, "IT Consulting",
                "Get expert guidance to shape your tech idea into a clear strategy.");
    }

    @GetMapping("/maintenance")
    public String maintenance(Model model) {
        return page(model, "Maintenance",
                "Keep your product running reliably with ongoing professional support.");
    }

    @GetMapping("/build")
    public String build(Model model) {
        return page(model, "Build",
                "Turn your idea into a working product; whether you're leading a business or building your own.");
    }

    @GetMapping("/learn")
    public String learn(Model model) {
        return page(model, "Learn",
                "Build the skills to understand, ship, and evolve modern software - for teams and for individuals.");
    }

    @GetMapping("/design")
    public String design(Model model) {
        return page(model, "Design",
                "Interfaces, design systems, and architecture that make products feel right - for businesses and for engineers.");
    }

    @GetMapping("/process-automation")
    public String processAutomation(Model model) {
        return page(model, "Process Automation",
                "Bots, AI agents, and workflows to automate your business processes.");
    }

    @GetMapping("/data-systems")
    public String dataSystems(Model model) {
        return page(model, "Data Systems",
                "Database design, query optimization, and reliable data pipelines.");
    }

    @GetMapping("/security")
    public String security(Model model) {
        return page(model, "Security",
                "Harden your systems with audits, pen testing, and security best practices.");
    }

    private String page(Model model, String title, String tagline) {
        model.addAttribute("title", title);
        model.addAttribute("tagline", tagline);
        return VIEW;
    }
}
