package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PosthogModelAdvice {

    private final PosthogProperties props;

    public PosthogModelAdvice(PosthogProperties props) {
        this.props = props;
    }

    @ModelAttribute("posthogKey")
    public String posthogKey() {
        return props.key();
    }
}
