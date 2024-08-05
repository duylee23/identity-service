package com.duyle.identity_service.controller;

import com.duyle.identity_service.dto.request.AuthenticationRequest;
import com.duyle.identity_service.dto.request.IntrospectRequest;
import com.duyle.identity_service.dto.request.LogoutRequest;
import com.duyle.identity_service.dto.request.RefreshTokenRequest;
import com.duyle.identity_service.dto.response.ApiResponse;
import com.duyle.identity_service.dto.response.AuthenticationResponse;
import com.duyle.identity_service.dto.response.IntrospectResponse;
import com.duyle.identity_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/log-in")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .message("Log-in successfully!")
                .build();
    }

    @PostMapping("/refresh-token")
    ApiResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .message("refresh token successfully!")
                .build();
    }

    @PostMapping(value = "/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .message("Token valid!")
                .build();
    }

    @PostMapping(value = "/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }


}
