package com.school.hotel.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data public class ProfileUpdateRequest { @Size(max=64) private String name; @Pattern(regexp="^$|^1[3-9]\\d{9}$") private String phone; @Email @Size(max=128) private String email; @Size(max=500) private String avatar; private String idCard; }
