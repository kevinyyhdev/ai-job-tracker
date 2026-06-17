package com.kevin.jobtracker.user;

import com.kevin.jobtracker.common.api.ApiResponse;
import com.kevin.jobtracker.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(new UserResponse(currentUser.getId(), currentUser.getEmail(), currentUser.getFullName()));
    }
}
