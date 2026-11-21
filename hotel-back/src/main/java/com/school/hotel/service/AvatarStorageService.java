package com.school.hotel.service;

import com.school.hotel.common.BusinessException;
import com.school.hotel.config.UploadProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private final UploadProperties properties;
    public AvatarStorageService(UploadProperties properties) { this.properties=properties; }
    private Path root() { return Path.of(properties.getPath()).toAbsolutePath().normalize().resolve("avatars"); }

    public String store(MultipartFile file) throws IOException {
        if(file.isEmpty()) throw new BusinessException(400,"图片文件为空，请重新选择");
        if(file.getSize()>5*1024*1024) throw new BusinessException(400,"头像不能超过 5MB");
        if(!"image/png".equals(file.getContentType()) && !"image/jpeg".equals(file.getContentType()))
            throw new BusinessException(400,"请选择 JPG 或 PNG 图片");
        BufferedImage original;
        try(var input=ImageIO.createImageInputStream(file.getInputStream())) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new BusinessException(400,"图片内容无效，请重新选择 JPG 或 PNG 图片");
            var reader=readers.next();
            try {
                String format=reader.getFormatName();
                if(!"PNG".equalsIgnoreCase(format) && !"JPEG".equalsIgnoreCase(format))
                    throw new BusinessException(400,"请选择 JPG 或 PNG 图片");
                reader.setInput(input);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || width>4096 || height>4096 || (long)width*height>16_000_000)
                    throw new BusinessException(400,"图片尺寸过大，请使用 4096 像素以内的图片");
                original=reader.read(0);
            } finally {reader.dispose();}
        } catch(IOException | IllegalArgumentException exception) {
            throw new BusinessException(400,"图片无法读取，请重新选择完整的 JPG 或 PNG 图片");
        }
        int crop=Math.min(original.getWidth(),original.getHeight()),size=Math.min(crop,512);
        var avatar=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        var graphics=avatar.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int x=(original.getWidth()-crop)/2,y=(original.getHeight()-crop)/2;
            graphics.drawImage(original,0,0,size,size,x,y,x+crop,y+crop,null);
        } finally {graphics.dispose();}
        Files.createDirectories(root());
        String name=UUID.randomUUID()+".png";
        ImageIO.write(avatar,"png",root().resolve(name).toFile());
        return "/uploads/avatars/"+name;
    }

    public void validateReference(String avatar) {
        if(avatar.matches("/avatars/(forest|ocean|sunshine|dusk|sand|sky)\\.svg")) return;
        if(avatar.matches("/uploads/avatars/[a-f0-9-]{36}\\.png")
                && Files.isRegularFile(root().resolve(avatar.substring(avatar.lastIndexOf('/')+1)))) return;
        throw new BusinessException(400,"请选择默认头像或重新上传头像");
    }
}
