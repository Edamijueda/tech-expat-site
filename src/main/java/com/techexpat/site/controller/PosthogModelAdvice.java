package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PosthogModelAdvice {

    private final PosthogProperties props;

    public PosthogModelAdvice(PosthogProperties props) {
        this.props = props;
    }

    /**
     * Empty behind a sign-in, which leaves the layout's existing emptiness check to drop
     * the snippet. Autocapture reads a clicked element's attributes, and the signed-in
     * pages carry the customer's own words — document descriptions in the dashboard's
     * tooltips, their email addresses in the admin list.
     *
     * <p>Decided on the principal rather than a list of paths so that a page put behind
     * authentication later is covered without anyone having to remember this.
     */
    @ModelAttribute("posthogKey")
    public String posthogKey() {
        return signedIn() ? "" : props.key();
    }

    private static boolean signedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }
}
