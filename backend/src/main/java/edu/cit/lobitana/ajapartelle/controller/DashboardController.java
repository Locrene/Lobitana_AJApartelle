package edu.cit.lobitana.ajapartelle.controller;

import edu.cit.lobitana.ajapartelle.entity.Role;
import edu.cit.lobitana.ajapartelle.entity.User;
import edu.cit.lobitana.ajapartelle.repository.UserRepository;
import edu.cit.lobitana.ajapartelle.security.AuthUser;
import edu.cit.lobitana.ajapartelle.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Role dashboards. URL rules in SecurityConfig AND @PreAuthorize enforce the SRS Appendix F matrix.
 * Booking/payment/expense modules arrive in later increments; they are listed as upcoming modules.
 */
@RestController
public class DashboardController {
    private final UserService userService;
    private final UserRepository users;

    public DashboardController(UserService userService, UserRepository users) {
        this.userService = userService;
        this.users = users;
    }

    @GetMapping("/api/v1/guest/dashboard")
    @PreAuthorize("hasRole('GUEST')")
    public Map<String, Object> guest(@AuthenticationPrincipal AuthUser me) {
        User u = userService.getById(me.id());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", "GUEST");
        body.put("fullName", u.getFullName());
        body.put("email", u.getEmail());
        body.put("mobileNumber", u.getMobileNumber());
        body.put("memberSince", u.getCreatedAt());
        body.put("upcomingModules", List.of("Browse rooms and availability", "Request a booking", "My bookings"));
        return body;
    }

    @GetMapping("/api/v1/staff/dashboard")
    @PreAuthorize("hasAnyRole('STAFF','OWNER')")
    public Map<String, Object> staff(@AuthenticationPrincipal AuthUser me) {
        User u = userService.getById(me.id());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", u.getRole().name());
        body.put("fullName", u.getFullName());
        body.put("activeStaff", users.countByRoleAndActive(Role.STAFF, true));
        body.put("upcomingModules", List.of("Today: pending requests, arrivals, in-house, departures",
                "Walk-in / phone booking", "Room status board", "Payments", "Expenses"));
        return body;
    }

    @GetMapping("/api/v1/owner/dashboard")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> owner(@AuthenticationPrincipal AuthUser me) {
        User u = userService.getById(me.id());
        Map<String, Object> accounts = new LinkedHashMap<>();
        accounts.put("guests", users.countByRole(Role.GUEST));
        accounts.put("staff", users.countByRole(Role.STAFF));
        accounts.put("activeStaff", users.countByRoleAndActive(Role.STAFF, true));
        accounts.put("owners", users.countByRole(Role.OWNER));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", "OWNER");
        body.put("fullName", u.getFullName());
        body.put("accounts", accounts);
        body.put("upcomingModules", List.of("Occupancy, income and expenses", "Monthly reports",
                "Rooms and rates", "Activity log", "Sync status"));
        return body;
    }
}
