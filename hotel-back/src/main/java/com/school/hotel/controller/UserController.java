package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.dto.*;
import com.school.hotel.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController @RequestMapping("/api/user")
public class UserController {
    private final UserService service;
    public UserController(UserService service){this.service=service;}
    @PostMapping("/avatar") public Result<?> avatar(@RequestPart("file") MultipartFile file) throws java.io.IOException {
        return Result.success(Map.of("url",service.uploadAvatar(file)));
    }
    @PostMapping("/login") public Result<?> login(@Valid @RequestBody LoginRequest request){return Result.success(service.login(request));}
    @PostMapping("/register") public Result<?> register(@Valid @RequestBody RegisterRequest request){return Result.success(service.register(request));}
    @PostMapping("/refresh") public Result<?> refresh(@Valid @RequestBody RefreshTokenRequest request){return Result.success(service.refresh(request));}
    @PostMapping("/logout") public Result<?> logout(@RequestBody(required=false) RefreshTokenRequest request){if(request!=null)service.logout(request.getRefreshToken());return Result.success(null);}
    @GetMapping("/profile") public Result<?> profile(){return Result.success(service.profile());}
    @PutMapping("/profile") public Result<?> update(@Valid @RequestBody ProfileUpdateRequest request){return Result.success(service.updateProfile(request));}
    @GetMapping("/favorites") public Result<?> favorites(){return Result.success(service.favorites());}
    @PostMapping("/favorites/{hotelId}") public Result<?> favorite(@PathVariable Long hotelId){return Result.success(service.toggleFavorite(hotelId));}
    @PutMapping("/favorites/{hotelId}") public Result<?> addFavorite(@PathVariable Long hotelId){return Result.success(service.setFavorite(hotelId,true));}
    @DeleteMapping("/favorites/{hotelId}") public Result<?> removeFavorite(@PathVariable Long hotelId){return Result.success(service.setFavorite(hotelId,false));}
    @GetMapping("/comments") public Result<?> comments(){return Result.success(service.comments());}
    @PostMapping("/comments") public Result<?> createComment(@Valid @RequestBody CommentSaveRequest request){return Result.success(service.createComment(request));}
    @PutMapping("/comments/{id}") public Result<?> updateComment(@PathVariable Long id,@Valid @RequestBody CommentSaveRequest request){return Result.success(service.updateComment(id,request));}
    @DeleteMapping("/comments/{id}") public Result<?> deleteComment(@PathVariable Long id){service.deleteComment(id);return Result.success(null);}
}
