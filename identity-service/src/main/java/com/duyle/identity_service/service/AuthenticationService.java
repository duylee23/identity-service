package com.duyle.identity_service.service;

import com.duyle.identity_service.dto.request.AuthenticationRequest;
import com.duyle.identity_service.dto.request.IntrospectRequest;
import com.duyle.identity_service.dto.request.LogoutRequest;
import com.duyle.identity_service.dto.request.RefreshTokenRequest;
import com.duyle.identity_service.dto.response.AuthenticationResponse;
import com.duyle.identity_service.dto.response.IntrospectResponse;
import com.duyle.identity_service.dto.response.UserResponse;
import com.duyle.identity_service.entity.InvalidatedToken;
import com.duyle.identity_service.entity.User;
import com.duyle.identity_service.exception.AppException;
import com.duyle.identity_service.exception.ErrorCode;
import com.duyle.identity_service.repository.InvalidatedTokenRepository;
import com.duyle.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.impl.HMAC;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;
    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;
    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    public AuthenticationResponse authenticate (AuthenticationRequest authenticationRequest) {
        var user = this.userRepository.findByUsername(authenticationRequest.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        boolean authenticated = passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword());

        if(!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .isAuthenticated(true)
                .build();
    }

    public IntrospectResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;
        try{
            verifyToken(token, false);
        }catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    //refresh token
    public AuthenticationResponse refreshToken (RefreshTokenRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyToken(request.getToken(), true);
        var jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        var expireTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jwtId)
                .expireTime(expireTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
        String username = signedJWT.getJWTClaimsSet().getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .isAuthenticated(true)
                .build();
    }



    public void logout (LogoutRequest request) throws ParseException, JOSEException {
        try{
            var signedToken = verifyToken(request.getToken(), true);
            String jwtId = signedToken.getJWTClaimsSet().getJWTID();
            Date expireTime = signedToken.getJWTClaimsSet().getExpirationTime();
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jwtId)
                    .expireTime(expireTime)
                    .build();
            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e){
            log.warn("Token has already expired");
        }
    }

    private String generateToken(User user) {
        //header
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        //build payload
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("duyle.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    //Json Web Signature (JWS)
    private SignedJWT verifyToken (String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expireTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet()
                                    .getIssueTime()
                                    .toInstant()
                                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();


        var isVerified = signedJWT.verify(verifier);

        if(!(isVerified && expireTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository
                .existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }





    //build a scope for user's role
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
//        if(!CollectionUtils.isEmpty(user.getRoles())){
//            user.getRoles().forEach(role -> {
//                stringJoiner.add("ROLE_" + role.getName());
//                if(!CollectionUtils.isEmpty(role.getPermissions())) {
//                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
//                }
//            });
//        }
        return stringJoiner.toString();
    }
}
