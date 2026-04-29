package com.example.demo.controller;


import com.example.demo.dto.LoginUserDto;
import com.example.demo.dto.RegisterUserDto;
import com.example.demo.dto.ResendVerificationDto;
import com.example.demo.dto.VerifyUserDto;
import com.example.demo.model.User;
import com.example.demo.responses.LoginResponse;
import com.example.demo.responses.MessageResponse;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto){
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        authenticationService.verifyUser(verifyUserDto);
        MessageResponse response = new MessageResponse("Account verified successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        authenticationService.resendVerificationCode(email);
        MessageResponse response = new MessageResponse("Verification code sent");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-body")
    public ResponseEntity<?> resendVerificationCodeBody(@Valid @RequestBody ResendVerificationDto resendVerificationDto) {
        authenticationService.resendVerificationCode(resendVerificationDto.getEmail());
        MessageResponse response = new MessageResponse("Verification code sent");
        return ResponseEntity.ok(response);
    }
    


}