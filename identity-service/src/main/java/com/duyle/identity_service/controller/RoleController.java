package com.duyle.identity_service.controller;

import com.duyle.identity_service.dto.request.RoleRequest;
import com.duyle.identity_service.dto.response.ApiResponse;
import com.duyle.identity_service.dto.response.RoleResponse;
import com.duyle.identity_service.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/roles")
@Slf4j
public class RoleController {
    RoleService roleService;

    @PostMapping
    ApiResponse<RoleResponse> create(@RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.create(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @DeleteMapping("/{roleName}")
    ApiResponse<Void> delete(@PathVariable String roleName) {
        roleService.delete(roleName);
        return ApiResponse.<Void>builder().build();
    }
}
