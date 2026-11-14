package com.techexpat.site.controller;

import com.techexpat.site.model.KbdUser;
import com.techexpat.site.model.BizDoc;
import com.techexpat.site.service.KbdUserService;
import com.techexpat.site.service.BizDocService;
import com.techexpat.site.service.BizDocService.BizDocValidationException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tool/keep-business-docs")
public class KbdBizDocController {

    private static final String DASHBOARD = "redirect:/tool/keep-business-docs/dashboard";

    private final BizDocService docService;
    private final KbdUserService userService;

    public KbdBizDocController(BizDocService docService, KbdUserService userService) {
        this.docService = docService;
        this.userService = userService;
    }

    @PostMapping("/docs")
    public String upload(@RequestParam("description") String description,
                         @RequestParam("file") MultipartFile file,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            docService.upload(currentUserId(authentication), description, file);
            redirectAttributes.addFlashAttribute("infoMessage", "Document kept.");
        } catch (BizDocValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("description", description);
        }
        return DASHBOARD;
    }

    @GetMapping("/docs/{id}/delete")
    public String confirmDelete(@PathVariable String id, Authentication authentication,
                                Model model) {
        model.addAttribute("doc", ownedBizDoc(id, authentication));
        return "tool/keep-business-docs/delete-doc";
    }

    @PostMapping("/docs/{id}/delete")
    public String delete(@PathVariable String id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        docService.delete(ownedBizDoc(id, authentication));
        redirectAttributes.addFlashAttribute("infoMessage", "Document deleted.");
        return DASHBOARD;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> preview(@PathVariable String id, Authentication authentication) {
        return serve(ownedBizDoc(id, authentication), ContentDisposition.inline());
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id, Authentication authentication) {
        return serve(ownedBizDoc(id, authentication), ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> serve(BizDoc doc, ContentDisposition.Builder disposition) {
        byte[] content = docService.contentOf(doc);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.filename(downloadName(doc)).build().toString())
                .body(content);
    }

    /**
     * Scoped by user id, and a miss is a 404 rather than a 403 so document ids can't be
     * probed for existence.
     */
    private BizDoc ownedBizDoc(String id, Authentication authentication) {
        return docService.findOwned(id, currentUserId(authentication))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String currentUserId(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .map(KbdUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** The original name came from the user's device, so it's stripped to a bare filename. */
    private static String downloadName(BizDoc doc) {
        String original = doc.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "document" + extensionFor(doc.getContentType());
        }
        String bare = original.replaceAll(".*[/\\\\]", "").replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (bare.isEmpty()) {
            return "document" + extensionFor(doc.getContentType());
        }
        return bare.length() > 100 ? bare.substring(0, 100) : bare;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/heic" -> ".heic";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "That file is too large to keep.");
        return DASHBOARD;
    }
}
