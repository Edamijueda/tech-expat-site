package com.techexpat.site.controller;

import com.techexpat.site.model.KbrUser;
import com.techexpat.site.model.Receipt;
import com.techexpat.site.service.KbrUserService;
import com.techexpat.site.service.ReceiptService;
import com.techexpat.site.service.ReceiptService.ReceiptValidationException;

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
@RequestMapping("/tool/keep-business-receipt")
public class KbrReceiptController {

    private static final String DASHBOARD = "redirect:/tool/keep-business-receipt/dashboard";

    private final ReceiptService receiptService;
    private final KbrUserService userService;

    public KbrReceiptController(ReceiptService receiptService, KbrUserService userService) {
        this.receiptService = receiptService;
        this.userService = userService;
    }

    @PostMapping("/receipts")
    public String upload(@RequestParam("description") String description,
                         @RequestParam("file") MultipartFile file,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            receiptService.upload(currentUserId(authentication), description, file);
            redirectAttributes.addFlashAttribute("infoMessage", "Receipt kept.");
        } catch (ReceiptValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("description", description);
        }
        return DASHBOARD;
    }

    @GetMapping("/receipts/{id}/delete")
    public String confirmDelete(@PathVariable String id, Authentication authentication,
                                Model model) {
        model.addAttribute("receipt", ownedReceipt(id, authentication));
        return "tool/keep-business-receipt/delete-receipt";
    }

    @PostMapping("/receipts/{id}/delete")
    public String delete(@PathVariable String id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        receiptService.delete(ownedReceipt(id, authentication));
        redirectAttributes.addFlashAttribute("infoMessage", "Receipt deleted.");
        return DASHBOARD;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> preview(@PathVariable String id, Authentication authentication) {
        return serve(ownedReceipt(id, authentication), ContentDisposition.inline());
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id, Authentication authentication) {
        return serve(ownedReceipt(id, authentication), ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> serve(Receipt receipt, ContentDisposition.Builder disposition) {
        byte[] content = receiptService.contentOf(receipt);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.filename(downloadName(receipt)).build().toString())
                .body(content);
    }

    /**
     * Scoped by user id, and a miss is a 404 rather than a 403 so receipt ids can't be
     * probed for existence.
     */
    private Receipt ownedReceipt(String id, Authentication authentication) {
        return receiptService.findOwned(id, currentUserId(authentication))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String currentUserId(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .map(KbrUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** The original name came from the user's device, so it's stripped to a bare filename. */
    private static String downloadName(Receipt receipt) {
        String original = receipt.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "receipt" + extensionFor(receipt.getContentType());
        }
        String bare = original.replaceAll(".*[/\\\\]", "").replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (bare.isEmpty()) {
            return "receipt" + extensionFor(receipt.getContentType());
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
