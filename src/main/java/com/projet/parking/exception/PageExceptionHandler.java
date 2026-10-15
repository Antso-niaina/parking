package com.projet.parking.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.projet.parking.controller")
public class PageExceptionHandler {

    @ExceptionHandler({OperationNonAutoriseeException.class, ResourceNotFoundException.class})
    public String erreurMetier(RuntimeException e, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("erreur", e.getMessage());
        return "redirect:" + pagePrecedente(request);
    }


    private String pagePrecedente(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                String path = URI.create(referer).getPath();
                if (path != null && path.startsWith("/") && !path.startsWith("//")) {
                    return path;
                }
            } catch (IllegalArgumentException ignored) {

            }
        }
        return "/";
    }
}