package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.InviteCustomerRequest;
import com.national.utility.billing.dto.request.InviteStaffRequest;
import com.national.utility.billing.dto.request.UpdateUserRequest;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.RolePermissionResponse;
import com.national.utility.billing.dto.response.UserResponse;
import com.national.utility.billing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.getAllUsers(pageable)));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List what each role can view and change")
    public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> getRolePermissions() {
        return ResponseEntity.ok(ApiResponse.success("Role permissions retrieved", userService.getRolePermissions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userService.getUserById(id)));
    }

    @PostMapping("/invite/operator")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Invite an operator")
    public ResponseEntity<ApiResponse<UserResponse>> inviteOperator(
            @Valid @RequestBody InviteStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator invited successfully", userService.inviteOperator(request)));
    }

    @PostMapping("/invite/finance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Invite a finance officer")
    public ResponseEntity<ApiResponse<UserResponse>> inviteFinance(
            @Valid @RequestBody InviteStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Finance user invited successfully", userService.inviteFinance(request)));
    }

    @PostMapping("/invite/customer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Invite a customer with billing profile")
    public ResponseEntity<ApiResponse<UserResponse>> inviteCustomer(
            @Valid @RequestBody InviteCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer invited successfully", userService.inviteCustomer(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a managed user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a managed user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}
