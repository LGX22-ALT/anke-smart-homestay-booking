package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.HotelApplication;
import com.school.hotel.config.JwtTokenService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=HotelApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:profileAvatar;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.sql.init.mode=always","spring.sql.init.schema-locations=classpath:booking-schema.sql",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "hotel.upload.path=./target/profile-test-uploads","hotel.cors.allowed-origins=http://127.0.0.1:2231",
    "hotel.order.payment-timeout-initial-delay-ms=3600000","logging.level.com.school.hotel=warn"})
@ActiveProfiles("test") @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class ProfileAvatarTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired JwtTokenService jwt;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired UserService profiles;
    @org.springframework.boot.test.mock.mockito.SpyBean com.school.hotel.mapper.UserMapper users;
    @LocalServerPort int port;
    @BeforeEach void seed() {
        db.update("DELETE FROM tb_user");
        for(int id=1;id<=2;id++) db.update("INSERT INTO tb_user(id,phone,email,password,name,avatar,id_card,role,status) VALUES(?,?,?,?,?,?,?,?,1)",
            id,"1380000000"+id,"profile"+id+"@example.test",encoder.encode("ProfileTest@123"),"Original "+id,
            "/avatars/forest.svg","verified-fixture",id==1?"user":"merchant");
    }
    String auth(int id,String role) {return "Bearer "+jwt.createAccessToken((long)id,role);}
    @ParameterizedTest @CsvSource({"1,user","2,merchant"})
    void savesOwnNameAndAvatarAndReturnsThemAfterLogin(int id,String role) throws Exception {
        mvc.perform(put("/api/user/profile").header("Authorization",auth(id,role)).contentType("application/json")
            .content(json.writeValueAsString(Map.of("name","  New Name  ","avatar","/avatars/ocean.svg","id",3-id,"role","admin"))))
            .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.name").value("New Name"))
            .andExpect(jsonPath("$.data.verified").value(true)).andExpect(jsonPath("$.data.avatar").value("/avatars/ocean.svg"));
        assertEquals("Original "+(3-id),db.queryForObject("SELECT name FROM tb_user WHERE id=?",String.class,3-id));
        mvc.perform(post("/api/user/login").contentType("application/json").content(json.writeValueAsString(
            Map.of("phone","1380000000"+id,"password","ProfileTest@123"))))
            .andExpect(jsonPath("$.data.userInfo.name").value("New Name"))
            .andExpect(jsonPath("$.data.userInfo.avatar").value("/avatars/ocean.svg"))
            .andExpect(jsonPath("$.data.role").value(role));
    }
    @Test void partialUpdatePreservesOtherProfileFields() throws Exception {
        mvc.perform(put("/api/user/profile").header("Authorization",auth(1,"user")).contentType("application/json")
            .content("{\"name\":\"Renamed\"}"))
            .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.avatar").value("/avatars/forest.svg"))
            .andExpect(jsonPath("$.data.verified").value(true)).andExpect(jsonPath("$.data.email").value("profile1@example.test"));
    }
    @Test void concurrentPartialUpdatesKeepBothChanges() throws Exception {
        var reads=new java.util.concurrent.CountDownLatch(2);
        var count=new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer(call -> {
            var result=db.queryForObject("SELECT * FROM tb_user WHERE id=1",(rs,row) -> {
                var user=new com.school.hotel.entity.User();
                user.setId(1L);user.setName(rs.getString("name"));user.setAvatar(rs.getString("avatar"));
                user.setPhone(rs.getString("phone"));user.setEmail(rs.getString("email"));
                user.setIdCard(rs.getString("id_card"));user.setRole(rs.getString("role"));user.setStatus(rs.getInt("status"));
                return user;
            });
            if(count.incrementAndGet()<=2) {reads.countDown();assertTrue(reads.await(5,TimeUnit.SECONDS));}
            return result;
        }).when(users).selectById(1L);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var tasks=new ArrayList<java.util.concurrent.Future<?>>();
            for(boolean name:List.of(true,false)) tasks.add(pool.submit(() -> {
                var context=org.springframework.security.core.context.SecurityContextHolder.getContext();
                context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "1",null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
                try {
                    var request=new com.school.hotel.dto.ProfileUpdateRequest();
                    if(name) request.setName("Concurrent Name");else request.setAvatar("/avatars/ocean.svg");
                    profiles.updateProfile(request);
                } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
            }));
            for(var task:tasks) task.get(10,TimeUnit.SECONDS);
            assertEquals("Concurrent Name",db.queryForObject("SELECT name FROM tb_user WHERE id=1",String.class));
            assertEquals("/avatars/ocean.svg",db.queryForObject("SELECT avatar FROM tb_user WHERE id=1",String.class));
        } finally {pool.shutdownNow();}
    }
    @Test void rejectsBlankLongNamesAndUntrustedAvatarPaths() throws Exception {
        for(String name:List.of("   ","x".repeat(65))) mvc.perform(put("/api/user/profile").header("Authorization",auth(1,"user"))
            .contentType("application/json").content(json.writeValueAsString(Map.of("name",name))))
            .andExpect(jsonPath("$.code").value(400));
        for(String avatar:List.of("https://elsewhere.test/a.png","/avatars/other.svg","/uploads/avatars/../private.png",
            "/uploads/avatars/00000000-0000-0000-0000-000000000000.png"))
            mvc.perform(put("/api/user/profile").header("Authorization",auth(1,"user")).contentType("application/json")
                .content(json.writeValueAsString(Map.of("avatar",avatar)))).andExpect(jsonPath("$.code").value(400));
    }
    @ParameterizedTest @CsvSource({"png,image/png","jpg,image/jpeg"})
    void uploadsRealImageAndPersistsReachableNormalizedAvatar(String format,String contentType) throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(80,40,BufferedImage.TYPE_INT_RGB),format,out);
        var response=mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file","picture."+format,contentType,out.toByteArray()))
            .header("Authorization",auth(2,"merchant"))).andExpect(jsonPath("$.code").value(200)).andReturn();
        String url=json.readTree(response.getResponse().getContentAsString()).path("data").path("url").asText();
        assertTrue(url.matches("/uploads/avatars/[a-f0-9-]{36}\\.png"));
        mvc.perform(put("/api/user/profile").header("Authorization",auth(2,"merchant")).contentType("application/json")
            .content(json.writeValueAsString(Map.of("avatar",url)))).andExpect(jsonPath("$.data.avatar").value(url));
        var file=mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var image=ImageIO.read(new java.io.ByteArrayInputStream(file));
        assertEquals(image.getWidth(),image.getHeight());assertTrue(image.getWidth()<=512);
    }
    @Test void rejectsInvalidOversizedAndUnsupportedUploads() throws Exception {
        for(var file:List.of(new MockMultipartFile("file","fake.png","image/png","not an image".getBytes()),
            new MockMultipartFile("file","empty.png","image/png",new byte[0]),
            new MockMultipartFile("file","vector.svg","image/svg+xml","<svg/>".getBytes()),
            new MockMultipartFile("file","big.png","image/png",new byte[5*1024*1024+1])))
            mvc.perform(multipart("/api/user/avatar").file(file).header("Authorization",auth(1,"user")))
                .andExpect(jsonPath("$.code").value(400));
    }
    @Test void rejectsImageDimensionsBeforeFullDecode() throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4097,1,BufferedImage.TYPE_INT_RGB),"png",out);
        mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file","wide.png","image/png",out.toByteArray()))
            .header("Authorization",auth(1,"user"))).andExpect(jsonPath("$.code").value(400));
    }
    @Test void profileAndAvatarRequireAuthentication() throws Exception {
        mvc.perform(put("/api/user/profile").contentType("application/json").content("{\"name\":\"No\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file","a.png","image/png",new byte[1])))
            .andExpect(status().isUnauthorized());
    }
    @Test @EnabledIfEnvironmentVariable(named="RUN_PROFILE_BROWSER_TESTS",matches="true")
    void browserProfiles() throws Exception {
        var root=Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        var log=root.resolve("hotel-out/test-artifacts/profile-avatar-browser.log");Files.createDirectories(log.getParent());
        var builder=new ProcessBuilder("python",root.resolve("hotel-out/scripts/profile_avatar_test.py").toString());
        builder.directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("PROFILE_BACKEND_URL","http://127.0.0.1:"+port);
        var process=builder.start();
        if(!process.waitFor(180,TimeUnit.SECONDS)){process.descendants().forEach(ProcessHandle::destroy);process.destroyForcibly();fail("Profile browser timeout");}
        assertEquals(0,process.exitValue(),"See profile-avatar-browser.log");
    }
}
