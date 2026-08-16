# CLAUDE.md

TechExpat site — agency portfolio and client portal. Spring Boot 4.0.2, Java 25, Thymeleaf, Spring Security 6, Gradle 9.

## Build, test, run

- Run dev server: `./gradlew bootRun` (auto-activates `dev` profile)
- Run tests: `./gradlew test`
- Build jar: `./gradlew build`

## Layout

- `src/main/java/com/techexpat/` — application code, `Application.java` is the entry point
- `src/main/resources/templates/` — Thymeleaf views; layout at `layout/layout.html`, reusable pieces in `fragments/`
- `src/main/resources/static/` — CSS/JS assets (Bootstrap 5.3.8 via CDN)
- `src/main/resources/application*.yml` — base + `dev` + `prod` profiles
- `src/test/java/` — tests use `spring-boot-starter-webmvc-test`

## Conventions

- Spring Boot 4.x moved packages — see notable ones: `UserDetailsServiceAutoConfiguration` is in `org.springframework.boot.security.autoconfigure`, `@WebMvcTest` in `org.springframework.boot.webmvc.test.autoconfigure`
- Thymeleaf Layout Dialect: pages `layout:decorate="~{layout/layout}"` and fill `layout:fragment="content"`
- New service tiles on the landing page use the `fragments/service-btn :: serviceBtn(title, desc, href)` fragment

## Hard rules

- Java toolchain is pinned to 25 — do not downgrade
- Never commit `.env*`, credentials, or files under `secrets/`
- Deploy target is Railway; production runs with `-Dspring.profiles.active=prod`
