package com.school.hotel.service;

import com.school.hotel.HotelApplication;
import com.school.hotel.config.JwtTokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=HotelApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:h2:mem:merchantChat;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.sql.init.mode=always","spring.sql.init.schema-locations=classpath:booking-schema.sql",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "hotel.upload.path=./target/merchant-chat-uploads",
    "hotel.cors.allowed-origins=http://127.0.0.1:2236",
    "hotel.order.payment-timeout-initial-delay-ms=3600000","logging.level.com.school.hotel=warn"})
@ActiveProfiles("test") @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class MerchantChatTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired JwtTokenService jwt;
    @LocalServerPort int port;
    String auth(int id,String role) {return "Bearer "+jwt.createAccessToken((long)id,role);}
    @BeforeEach void seed() {
        db.update("DELETE FROM tb_private_message");db.update("DELETE FROM tb_hotel");
        db.update("DELETE FROM tb_merchant");db.update("DELETE FROM tb_user");
        for(int id=1;id<=4;id++) db.update("INSERT INTO tb_user(id,phone,password,name,avatar,role,status) VALUES(?,?,?,?,?,?,1)",
            id,"1381111000"+id,"unused-fixture","Name "+id,"/avatars/forest.svg",id<3?"user":"merchant");
        for(int id=1;id<=2;id++) {
            db.update("INSERT INTO tb_merchant(id,user_id,company_name,contact_name,status) VALUES(?,?,?,?,?)",id,id+2,"Stay "+id,"Contact","APPROVED");
            db.update("INSERT INTO tb_hotel(id,merchant_id,name,city,status,min_price) VALUES(?,?,?,?,?,?)",id,id,"Hotel "+id,"广州","ONLINE",200);
        }
    }
    @Test void savedMerchantProfileAppearsInPublicDetailAndListWithoutPrivateData() throws Exception {
        mvc.perform(put("/api/user/profile").header("Authorization",auth(3,"merchant"))
            .contentType("application/json").content("{\"name\":\"Updated host\",\"avatar\":\"/avatars/ocean.svg\"}"))
            .andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/hotels/1")).andExpect(jsonPath("$.data.merchant.avatar").value("/avatars/ocean.svg"))
            .andExpect(jsonPath("$.data.merchant.name").value("Updated host"))
            .andExpect(jsonPath("$.data.merchant.password").doesNotExist()).andExpect(jsonPath("$.data.merchant.phone").doesNotExist());
        mvc.perform(get("/api/hotels")).andExpect(jsonPath("$.data.list[0].merchant.avatar").value("/avatars/ocean.svg"));
    }
    @Test void conversationsArePersistentBidirectionalAndIsolated() throws Exception {
        mvc.perform(post("/api/messages/hotels/1").header("Authorization",auth(1,"user"))
            .contentType("application/json").content("{\"content\":\"有停车位吗？\"}"))
            .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.mine").value(true));
        mvc.perform(get("/api/merchant/messages").header("Authorization",auth(3,"merchant")))
            .andExpect(jsonPath("$.data[0].unreadCount").value(1));
        mvc.perform(post("/api/merchant/messages/1/1").header("Authorization",auth(3,"merchant"))
            .contentType("application/json").content("{\"content\":\"可以免费停车\"}"))
            .andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/messages").header("Authorization",auth(1,"user")))
            .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].merchantAvatar").value("/avatars/forest.svg"))
            .andExpect(jsonPath("$.data[0].unreadCount").value(1));
        mvc.perform(get("/api/messages/hotels/1").header("Authorization",auth(1,"user")))
            .andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[1].mine").value(false));
        mvc.perform(get("/api/messages").header("Authorization",auth(1,"user")))
            .andExpect(jsonPath("$.data[0].unreadCount").value(0));
        mvc.perform(get("/api/messages").header("Authorization",auth(2,"user"))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/messages/hotels/1").header("Authorization",auth(2,"user"))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/merchant/messages/1/1").header("Authorization",auth(4,"merchant"))).andExpect(jsonPath("$.code").value(403));
        mvc.perform(get("/api/messages")).andExpect(status().isUnauthorized());
    }
    @Test void rejectsBlankLongAndUnavailableMessages() throws Exception {
        for(String content:new String[]{"   ","x".repeat(1001)}) mvc.perform(post("/api/messages/hotels/1")
            .header("Authorization",auth(1,"user")).contentType("application/json").content("{\"content\":\""+content+"\"}"))
            .andExpect(jsonPath("$.code").value(400));
        db.update("UPDATE tb_hotel SET status='OFFLINE' WHERE id=1");
        mvc.perform(post("/api/messages/hotels/1").header("Authorization",auth(1,"user"))
            .contentType("application/json").content("{\"content\":\"Hello\"}"))
            .andExpect(jsonPath("$.code").value(404));
    }
    @Test @EnabledIfEnvironmentVariable(named="CHAT_BROWSER",matches="true")
    void browserMessagesAndMerchantAvatar() throws Exception {
        Path root=Path.of("..").toAbsolutePath().normalize();
        var builder=new ProcessBuilder("python",root.resolve("hotel-out/scripts/merchant_chat_test.py").toString());
        builder.environment().put("CHAT_BACKEND_URL","http://127.0.0.1:"+port);
        builder.environment().put("CHAT_USER_TOKEN",jwt.createAccessToken(1L,"user"));
        builder.environment().put("CHAT_MERCHANT_TOKEN",jwt.createAccessToken(3L,"merchant"));
        builder.environment().put("PYTHONUTF8","1");
        builder.redirectErrorStream(true).redirectOutput(root.resolve("hotel-out/test-artifacts/merchant-chat-browser.log").toFile());
        Process process=builder.start();
        try {
            Assertions.assertTrue(process.waitFor(180,TimeUnit.SECONDS),"Browser timeout");
            Assertions.assertEquals(0,process.exitValue(),"See merchant-chat-browser.log");
        } finally {if(process.isAlive()) process.destroyForcibly();}
    }
}
