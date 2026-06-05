package com.national.utility.billing.security;

import com.national.utility.billing.dto.response.ResourcePermissionResponse;
import com.national.utility.billing.dto.response.RolePermissionResponse;
import com.national.utility.billing.model.enums.UserRole;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RolePermissionCatalog {

    private static final Map<UserRole, List<ResourcePermissionResponse>> PERMISSIONS = buildPermissions();

    private RolePermissionCatalog() {
    }

    public static List<RolePermissionResponse> getAllRolePermissions() {
        return List.of(
                toRoleResponse(UserRole.ADMIN),
                toRoleResponse(UserRole.OPERATOR),
                toRoleResponse(UserRole.FINANCE),
                toRoleResponse(UserRole.CUSTOMER));
    }

    public static List<ResourcePermissionResponse> getPermissionsForRole(UserRole role) {
        return PERMISSIONS.getOrDefault(role, List.of());
    }

    private static RolePermissionResponse toRoleResponse(UserRole role) {
        return RolePermissionResponse.builder()
                .role(role)
                .description(roleDescription(role))
                .resources(getPermissionsForRole(role))
                .build();
    }

    private static String roleDescription(UserRole role) {
        return switch (role) {
            case ADMIN -> "Super-user — full access to every endpoint (users, billing, payments, reports, readings).";
            case OPERATOR -> "Field operations — registers customers/meters and records meter readings.";
            case FINANCE -> "Billing and collections — generates bills, processes payments, and exports reports.";
            case CUSTOMER -> "Self-service portal — registers own meters, views bills, pays, and receives notifications.";
        };
    }

    private static Map<UserRole, List<ResourcePermissionResponse>> buildPermissions() {
        Map<UserRole, List<ResourcePermissionResponse>> map = new EnumMap<>(UserRole.class);

        map.put(UserRole.ADMIN, List.of(
                resource("Users", "Manage staff and customer accounts", true, true, true, true),
                resource("Customers", "Customer registry", true, true, true, true),
                resource("Meters", "Meter registry", true, true, true, true),
                resource("Readings", "Submit and view readings", true, true, false, false),
                resource("Bills", "All bills", true, false, false, false),
                resource("Payments", "All payments — process and view", true, true, false, false),
                resource("Tariffs", "Utility pricing rules", true, true, true, false),
                resource("Reports", "CSV/Excel exports", true, false, false, false),
                resource("Notifications", "All notifications + overdue procedure", true, true, false, false),
                resource("Locations", "Rwanda address picker", true, false, false, false)));

        map.put(UserRole.OPERATOR, List.of(
                resource("Users", "Staff accounts", false, false, false, false),
                resource("Customers", "Customer registry", true, true, false, false),
                resource("Meters", "Meter registry", true, true, false, false),
                resource("Readings", "Record and view readings", true, true, false, false),
                resource("Bills", "Generated bills", false, false, false, false),
                resource("Payments", "Payment records", false, false, false, false),
                resource("Tariffs", "Utility pricing rules", true, false, false, false),
                resource("Reports", "CSV/Excel exports", false, false, false, false),
                resource("Notifications", "DB trigger/procedure messages", true, false, false, false),
                resource("Locations", "Rwanda address picker", true, false, false, false)));

        map.put(UserRole.FINANCE, List.of(
                resource("Users", "Staff accounts", false, false, false, false),
                resource("Customers", "Customer registry", true, false, false, false),
                resource("Meters", "Meter registry", true, false, false, false),
                resource("Readings", "Meter reading history", true, false, false, false),
                resource("Bills", "View, approve, or reject generated bills", true, true, true, false),
                resource("Payments", "Process, approve, reject, and view payments", true, true, true, false),
                resource("Tariffs", "Utility pricing rules", true, false, false, false),
                resource("Reports", "Totals summary + download bills, payments, customers", true, false, false, false),
                resource("Notifications", "DB trigger/procedure messages", true, true, false, false),
                resource("Locations", "Rwanda address picker", true, false, false, false)));

        map.put(UserRole.CUSTOMER, List.of(
                resource("Users", "Staff accounts", false, false, false, false),
                resource("Customers", "Customer registry", false, false, false, false),
                resource("Meters", "Own meters — register and view", true, true, false, false),
                resource("Readings", "Meter reading history", false, false, false, false),
                resource("Bills", "Own bills only", true, false, false, false),
                resource("Payments", "Pay own bills and view own payments", true, true, false, false),
                resource("Tariffs", "Utility pricing rules", false, false, false, false),
                resource("Reports", "CSV/Excel exports", false, false, false, false),
                resource("Notifications", "Own notifications + email delivery", true, false, false, false),
                resource("Locations", "Rwanda address picker", true, false, false, false)));

        return map;
    }

    private static ResourcePermissionResponse resource(
            String resource, String description, boolean view, boolean create, boolean update, boolean delete) {
        return ResourcePermissionResponse.builder()
                .resource(resource)
                .description(description)
                .canView(view)
                .canCreate(create)
                .canUpdate(update)
                .canDelete(delete)
                .build();
    }
}
