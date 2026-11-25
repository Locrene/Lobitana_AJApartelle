package edu.cit.lobitana.ajapartelle.dashboard.controller;

import edu.cit.lobitana.ajapartelle.auth.security.AuthUser;
import edu.cit.lobitana.ajapartelle.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Role dashboards. URL rules in SecurityConfig AND @PreAuthorize enforce the SRS Appendix F matrix
 * (FR-044, NFR-05).
 */
@RestController
@Tag(name = "Dashboard", description = "Role dashboards for guest, staff and owner (Appendix F)")
public class DashboardController {
    private final DashboardService dashboards;

    public DashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/api/v1/guest/dashboard")
    @PreAuthorize("hasRole('GUEST')")
    public Map<String, Object> guest(@AuthenticationPrincipal AuthUser me) {
        return dashboards.guest(me.id());
    }

    @GetMapping("/api/v1/staff/dashboard")
    @PreAuthorize("hasAnyRole('STAFF','OWNER')")
    public Map<String, Object> staff(@AuthenticationPrincipal AuthUser me) {
        return dashboards.staff(me.id());
    }

    @GetMapping("/api/v1/owner/dashboard")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> owner(@AuthenticationPrincipal AuthUser me) {
        return dashboards.owner(me.id());
    }
}
