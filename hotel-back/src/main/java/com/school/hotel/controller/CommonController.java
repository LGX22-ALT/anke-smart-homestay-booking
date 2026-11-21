package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.config.UploadProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/common") public class CommonController {
    private final UploadProperties properties; public CommonController(UploadProperties properties){this.properties=properties;}
    @PostMapping("/upload") public Result<?> upload(@RequestPart("file") MultipartFile file) throws Exception {if(file.isEmpty())return Result.error(400,"文件不能为空");String content=file.getContentType()==null?"":file.getContentType();if(!content.startsWith("image/"))return Result.error(400,"仅支持图片文件");Path root=Path.of(properties.getPath()).toAbsolutePath().normalize();Files.createDirectories(root);String ext=file.getOriginalFilename()!=null&&file.getOriginalFilename().contains(".")?file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')):".bin";String name=UUID.randomUUID()+ext;Files.copy(file.getInputStream(),root.resolve(name),StandardCopyOption.REPLACE_EXISTING);return Result.success(Map.of("url","/uploads/"+name,"name",name));}
}
