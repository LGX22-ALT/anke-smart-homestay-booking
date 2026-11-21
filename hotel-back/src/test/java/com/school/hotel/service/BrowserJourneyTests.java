package com.school.hotel.service;

import com.school.hotel.HotelApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_BROWSER_TESTS",matches="true")
@SpringBootTest(classes=HotelApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:browser;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.sql.init.mode=always","spring.sql.init.schema-locations=classpath:booking-schema.sql",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    // booking.test is the production-bundle HTTP transport used only by the isolated browser test.
    "hotel.cors.allowed-origins=http://127.0.0.1:2299,http://booking.test",
    "hotel.order.payment-timeout-initial-delay-ms=3600000"})
@ActiveProfiles("test")
class BrowserJourneyTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;
    @Test void browserJourneys() throws Exception {
        String password=encoder.encode("BrowserTest@123");
        for(int i=1;i<=4;i++) db.update("INSERT INTO tb_user(id,phone,password,name,role,status) VALUES(?,?,?,?,?,1)",i,
            "1380000000"+i,password,"Test "+i,i==1?"admin":i==2?"merchant":"user");
        db.update("INSERT INTO tb_merchant(id,user_id,company_name,status) VALUES(1,2,'Browser merchant','APPROVED')");
        db.update("INSERT INTO tb_hotel(id,merchant_id,name,city,address,type,status,min_price) VALUES(1,1,'Browser Stay','北京','Test address','酒店','ONLINE',100)");
        db.update("INSERT INTO tb_room_type(id,hotel_id,name,base_price,stock,max_guests) VALUES(1,1,'Browser Room',100,3,2)");
        for(int i=0;i<10;i++) db.update("INSERT INTO tb_room_calendar(room_type_id,calendar_date,price,available_stock,is_available) VALUES(1,?,100,3,1)",LocalDate.now().plusDays(i));
        db.update("INSERT INTO tb_order(id,order_no,user_id,hotel_id,room_type_id,check_in,check_out,nights,guests,total_amount,order_status,payment_status,create_time) VALUES(900,'EXPIRED-TEST',4,1,1,?,?,1,1,100,'PENDING_PAYMENT','UNPAID',?)",
            LocalDate.now(),LocalDate.now().plusDays(1),java.time.LocalDateTime.now().minusMinutes(31));
        db.update("UPDATE tb_room_calendar SET available_stock=2 WHERE calendar_date=?",LocalDate.now());
        var root=Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        java.nio.file.Files.createDirectories(root.resolve(".codex-artifacts"));
        var builder=System.getProperty("os.name").startsWith("Windows")
            ?new ProcessBuilder("cmd.exe","/c","npm","run","test:e2e")
            :new ProcessBuilder("npm","run","test:e2e");
        builder.directory(root.resolve("hotel-out").toFile()).redirectErrorStream(true);
        builder.redirectOutput(root.resolve(".codex-artifacts/optimization-playwright.log").toFile());
        builder.environment().put("E2E_BACKEND_URL","http://127.0.0.1:"+port);
        var process=builder.start();
        if(!process.waitFor(5,TimeUnit.MINUTES)) { process.descendants().forEach(ProcessHandle::destroy);process.destroyForcibly();fail("Browser tests timed out"); }
        assertEquals(0,process.exitValue(),"See hotel-out/test-results and playwright-report");
        assertEquals("REFUNDED",db.queryForObject("SELECT order_status FROM tb_order WHERE order_no <> 'EXPIRED-TEST' ORDER BY id LIMIT 1",String.class));
        assertEquals("CANCELLED",db.queryForObject("SELECT order_status FROM tb_order WHERE id=900",String.class));
        assertEquals(3,db.queryForObject("SELECT MIN(available_stock) FROM tb_room_calendar",Integer.class));
    }
}
