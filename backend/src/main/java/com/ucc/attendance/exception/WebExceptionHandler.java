package com.ucc.attendance.exception;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ucc.attendance.controller.WebController;

/**
 * Handles exceptions thrown by Thymeleaf @Controller endpoints.
 * <p>
 * The existing {@link GlobalExceptionHandler} uses @RestControllerAdvice which only
 * intercepts @RestController endpoints. Exceptions from WebController (a plain @Controller)
 * were falling through to Spring Boot's default Whitelabel Error Page.
 * </p>
 */
@ControllerAdvice(assignableTypes = WebController.class)
public class WebExceptionHandler {

    /**
     * Handles ApiException from WebController (e.g., UNAUTHORIZED, FORBIDDEN).
     * Clears any invalid JWT cookie and redirects to the login page with an error message.
     */
    @ExceptionHandler(ApiException.class)
    public String handleApiException(ApiException ex, RedirectAttributes redirectAttributes,
                                     HttpServletResponse response) {
        // Clear potentially invalid/expired JWT cookie
        if (isAuthError(ex)) {
            clearTokenCookie(response);
        }
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/?tab=login";
    }

    /**
     * Catches any unexpected exception from WebController.
     * Prevents the Whitelabel Error Page from showing.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes,
                                         HttpServletResponse response) {
        clearTokenCookie(response);
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        redirectAttributes.addFlashAttribute("error", message);
        return "redirect:/?tab=login";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Invalid request method. Please try again.");
        return "redirect:/";
    }

    private boolean isAuthError(ApiException ex) {
        return ex.getStatus().value() == 401 || ex.getStatus().value() == 403;
    }

    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
