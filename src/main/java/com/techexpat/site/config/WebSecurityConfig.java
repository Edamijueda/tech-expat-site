package com.techexpat.site.config;

import com.techexpat.site.service.KbrUserDetailsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({AdminProperties.class, KbrProperties.class})
public class WebSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

    /** 30 days: long enough that a trader isn't re-entering a PIN on every trip to the market. */
    private static final int REMEMBER_ME_DAYS = 30;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUserDetailsService(AdminProperties props,
                                                     PasswordEncoder encoder,
                                                     Environment env) {
        String username = props.username();
        String password = props.password();
        boolean missing = username == null || username.isBlank()
                || password == null || password.isBlank();

        if (missing) {
            if (isProd(env)) {
                throw new IllegalStateException(
                        "ADMIN_USERNAME and ADMIN_PASSWORD must be set when running the prod profile");
            }
            log.warn("ADMIN_USERNAME / ADMIN_PASSWORD not set — using admin/admin DEFAULTS. "
                    + "Do not deploy without setting these.");
            username = "admin";
            password = "admin";
        }

        UserDetails admin = User.withUsername(username)
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http,
                                                 UserDetailsService adminUserDetailsService,
                                                 PasswordEncoder encoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(adminUserDetailsService);
        provider.setPasswordEncoder(encoder);

        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(provider);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http,
                                                KbrUserDetailsService kbrUserDetailsService,
                                                PasswordEncoder encoder,
                                                KbrAuthFailureHandler failureHandler,
                                                KbrAuthSuccessHandler successHandler,
                                                KbrProperties kbrProps,
                                                Environment env) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(kbrUserDetailsService);
        provider.setPasswordEncoder(encoder);

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/tool/keep-business-receipt/dashboard/**",
                    "/tool/keep-business-receipt/setup",
                    "/tool/keep-business-receipt/receipts/**",
                    "/tool/keep-business-receipt/files/**"
                ).hasRole("KBR_USER")
                .anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/**"))
            .rememberMe(remember -> remember
                .key(rememberMeKey(kbrProps, env))
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("kbr-remember-me")
                .tokenValiditySeconds(REMEMBER_ME_DAYS * 24 * 60 * 60)
                .useSecureCookie(isProd(env))
                .userDetailsService(kbrUserDetailsService))
            .formLogin(form -> form
                .loginPage("/tool/keep-business-receipt/sign-in")
                .loginProcessingUrl("/tool/keep-business-receipt/sign-in")
                .usernameParameter("username")
                .passwordParameter("password")
                .failureHandler(failureHandler)
                .successHandler(successHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/tool/keep-business-receipt/sign-out")
                .logoutSuccessUrl("/tool/keep-business-receipt/sign-in?signed-out")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "kbr-remember-me")
                .permitAll())
            .authenticationProvider(provider);
        return http.build();
    }

    /**
     * A guessable key means forgeable login cookies, so prod refuses to start without one
     * rather than falling back to a value that is published in this repository.
     */
    private static String rememberMeKey(KbrProperties props, Environment env) {
        String key = props.rememberMeKey();
        if (key != null && !key.isBlank()) {
            return key;
        }
        if (isProd(env)) {
            throw new IllegalStateException(
                    "KBR_REMEMBER_ME_KEY must be set when running the prod profile");
        }
        log.warn("KBR_REMEMBER_ME_KEY not set — using a DEVELOPMENT-ONLY key. "
                + "Do not deploy without setting this.");
        return "kbr-dev-only-remember-me-key";
    }

    private static boolean isProd(Environment env) {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }
}
