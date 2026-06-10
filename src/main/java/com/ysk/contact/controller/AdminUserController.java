package com.ysk.contact.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ysk.contact.dto.AdminUserResponse;
import com.ysk.contact.dto.RoleChangeRequest;
import com.ysk.contact.service.AdminUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 회원 관리 API(관리자 전용 — SecurityConfig 에서 {@code hasRole(ADMIN)} 강제).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public List<AdminUserResponse> list() {
        return adminUserService.list();
    }

    @PostMapping("/{id}/approve")
    public AdminUserResponse approve(@PathVariable Long id) {
        return adminUserService.approve(id);
    }

    @PostMapping("/{id}/unlock")
    public AdminUserResponse unlock(@PathVariable Long id) {
        return adminUserService.unlock(id);
    }

    @PatchMapping("/{id}/role")
    public AdminUserResponse changeRole(@PathVariable Long id,
                                        @Valid @RequestBody RoleChangeRequest request,
                                        Authentication authentication) {
        return adminUserService.changeRole(id, request.role(), authentication.getName());
    }
}
