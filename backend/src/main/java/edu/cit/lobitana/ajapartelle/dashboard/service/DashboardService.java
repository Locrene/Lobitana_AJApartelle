package edu.cit.lobitana.ajapartelle.dashboard.service;

import edu.cit.lobitana.ajapartelle.user.entity.Role;
import edu.cit.lobitana.ajapartelle.user.entity.User;
import edu.cit.lobitana.ajapartelle.user.repository.UserRepository;
import edu.cit.lobitana.ajapartelle.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the role dashboards (SRS Appendix F). Booking, payment and expense figures
 * (FR-035, FR-021) arrive in later increments; they are listed as upcoming modules.
 */
@Service
public class DashboardService {
    private final UserService userService;
    private final UserRepository users;

    public DashboardService(UserService userService, UserRepository users) {
        this.userService = userService;
        this.users = users;
    }

    public Map<String, Object> guest(Long userId) {
        User u = userService.getById(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", "GUEST");
        body.put("fullName", u.getFullName());
        body.put("email", u.getEmail());
        body.put("mobileNumber", u.getMobileNumber());
        body.put("memberSince", u.getCreatedAt());
        body.put("upcomingModules", List.of("Browse rooms and availability", "Request a booking", "My bookings"));
        return body;
    }

    public Map<String, Object> staff(Long userId) {
        User u = userService.getById(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", u.getRole().name());
        body.put("fullName", u.getFullName());
        body.put("activeStaff", users.countByRoleAndActive(Role.STAFF, true));
        body.put("upcomingModules", List.of("Today: pending requests, arrivals, in-house, departures",
                "Walk-in / phone booking", "Room status board", "Payments", "Expenses"));
        return body;
    }

    public Map<String, Object> owner(Long userId) {
        User u = userService.getById(userId);
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
