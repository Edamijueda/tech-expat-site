package com.techexpat.site.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
@EnableConfigurationProperties(AdminProperties.class)
public class WebSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().permitAll())
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

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

    private static boolean isProd(Environment env) {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }
}
