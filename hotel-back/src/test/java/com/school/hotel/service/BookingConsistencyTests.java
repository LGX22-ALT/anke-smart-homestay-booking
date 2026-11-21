package com.school.hotel.service;

import com.school.hotel.HotelApplication;
import com.school.hotel.common.BusinessException;
import com.school.hotel.config.JwtTokenService;
import com.school.hotel.dto.OrderCreateRequest;
import com.school.hotel.dto.OrderInterventionRequest;
import com.school.hotel.entity.Order;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HotelApplication.class, properties = {
    "spring.datasource.url=${BOOKING_TEST_DB_URL:jdbc:h2:mem:booking;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000}",
    "spring.datasource.username=${BOOKING_TEST_DB_USER:sa}", "spring.datasource.password=${BOOKING_TEST_DB_PASSWORD:}",
    "spring.datasource.driver-class-name=${BOOKING_TEST_DB_DRIVER:org.h2.Driver}", "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:booking-schema.sql",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "hotel.order.payment-timeout-initial-delay-ms=3600000", "logging.level.com.school.hotel=warn"
})
@ActiveProfiles("test") @AutoConfigureMockMvc
class BookingConsistencyTests {
    @Autowired OrderService orders;
    @Autowired RoomInventoryService inventory;
    @Autowired HotelService search;
    @Autowired MerchantService merchants;
    @Autowired UserService users;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService jwt;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    final LocalDate start = LocalDate.now().plusDays(5);

    static void login(long id, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            String.valueOf(id), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    @BeforeEach void setup() {
        for (String table : List.of("tb_order_status_log", "tb_order_guest", "tb_favorite", "tb_order", "tb_room_calendar", "tb_room_type", "tb_hotel", "tb_merchant", "tb_user"))
            db.update("DELETE FROM " + table);
        db.update("INSERT INTO tb_user(id,phone,password,name,role,status) VALUES(1,'13800000001','test','admin','admin',1),(2,'13800000002','test','merchant','merchant',1),(3,'13800000003','test','guest','user',1)");
        db.update("INSERT INTO tb_merchant(id,user_id,company_name,status) VALUES(1,2,'test','APPROVED')");
        db.update("INSERT INTO tb_hotel(id,merchant_id,name,city,address,type,status,min_price) VALUES(1,1,'Test Stay','Test','Test','hotel','ONLINE',100)");
        db.update("INSERT INTO tb_room_type(id,hotel_id,name,base_price,stock,max_guests) VALUES(1,1,'room',100,1,2)");
        for(int i=0;i<2;i++) db.update("INSERT INTO tb_room_calendar(room_type_id,calendar_date,price,available_stock,is_available) VALUES(1,?,100,1,1)", start.plusDays(i));
        login(3,"USER");
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    OrderCreateRequest request() {
        var r=new OrderCreateRequest(); r.setHotelId(1L);r.setRoomTypeId(1L);
        r.setCheckIn(start);r.setCheckOut(start.plusDays(2));r.setGuests(1);
        r.setGuestName("Guest");r.setGuestPhone("13800000003"); return r;
    }
    int stock() { return db.queryForObject("SELECT SUM(available_stock) FROM tb_room_calendar",Integer.class); }
    Order paid() { Order o=orders.create(request()); return orders.mockPay(o.getId(),true); }
    List<Boolean> race(Callable<Boolean> task) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        Callable<Boolean> run=()->{login(3,"USER");gate.await();try{return task.call();}finally{SecurityContextHolder.clearContext();}};
        try {var a=pool.submit(run);var b=pool.submit(run);gate.countDown();return List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));}
        finally{pool.shutdownNow();}
    }
    @Test void lastRoomCannotBeOversold() throws Exception {
        var results=race(()->{try{orders.create(request());return true;}catch(BusinessException e){return false;}});
        assertEquals(1,results.stream().filter(Boolean::booleanValue).count());assertEquals(0,stock());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_order",Integer.class));
    }
    @Test void availabilityRejectsInvalidDatesAndHiddenHotels() {
        assertEquals(2,search.availability(1L,start,start.plusDays(2)).size());
        assertThrows(BusinessException.class,()->search.availability(1L,null,start));
        assertThrows(BusinessException.class,()->search.availability(1L,start,start));
        assertThrows(BusinessException.class,()->search.availability(1L,start, start.plusDays(31)));
        assertThrows(BusinessException.class,()->search.availability(1L,LocalDate.now().minusDays(1),start));
        db.update("UPDATE tb_hotel SET status='OFFLINE' WHERE id=1");
        assertEquals(404,assertThrows(BusinessException.class,()->search.availability(1L,start,start.plusDays(2))).getCode());
        assertEquals(404,assertThrows(BusinessException.class,()->search.availability(999L,start,start.plusDays(2))).getCode());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={
        "/api/hotels/1/availability", "/api/hotels/1/availability?checkIn=bad&checkOut=bad", "/api/hotels/abc"})
    void malformedPublicQueryReturnsClientError(String path) throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get(path)).andExpect(jsonPath("$.code").value(400));
    }
    @Test void databaseSearchRequiresAllNightsOfTheSameVisibleRoom() {
        var filter=new com.school.hotel.dto.HotelSearchRequest();
        filter.setCheckIn(start);filter.setCheckOut(start.plusDays(2));filter.setGuests(2);
        assertEquals(1,search.list(filter).getTotal());
        db.update("UPDATE tb_room_calendar SET is_available=0 WHERE calendar_date=?",start.plusDays(1));
        assertEquals(0,search.list(filter).getTotal());
        db.update("INSERT INTO tb_room_type(id,hotel_id,name,base_price,stock,max_guests) VALUES(2,1,'other',100,1,2)");
        db.update("INSERT INTO tb_room_calendar(room_type_id,calendar_date,price,available_stock,is_available) VALUES(2,?,100,1,1)",start.plusDays(1));
        assertEquals(0,search.list(filter).getTotal());
        db.update("UPDATE tb_room_calendar SET is_available=1 WHERE room_type_id=1");
        filter.setGuests(3);assertEquals(0,search.list(filter).getTotal());
        filter.setGuests(2);filter.setCity("Other");assertEquals(0,search.list(filter).getTotal());
        filter.setCity("Test");filter.setBed("' OR 1=1 --");assertEquals(0,search.list(filter).getTotal());
        filter.setBed(null);db.update("UPDATE tb_room_type SET is_deleted=1 WHERE id=1");
        assertEquals(0,search.list(filter).getTotal());
    }
    @Test void refreshTokenCanOnlyBeConsumedOnceConcurrently() throws Exception {
        db.update("UPDATE tb_user SET password=? WHERE id=3",encoder.encode("Test@123"));
        var loginRequest=new com.school.hotel.dto.LoginRequest();loginRequest.setPhone("13800000003");loginRequest.setPassword("Test@123");
        String token=(String)users.login(loginRequest).get("refreshToken");
        var results=race(()->{var r=new com.school.hotel.dto.RefreshTokenRequest();r.setRefreshToken(token);
            try {users.refresh(r);return true;}catch(BusinessException e){return false;}});
        assertEquals(1,results.stream().filter(Boolean::booleanValue).count());
    }
    @Test void refreshTokensIssuedTogetherHaveUniqueIdentifiers() {
        assertEquals(10,java.util.stream.IntStream.range(0,10).mapToObj(i->jwt.createRefreshToken(3L,"user")).distinct().count());
    }
    @Test void missingCalendarCannotBeBooked() {
        db.update("DELETE FROM tb_room_calendar");
        assertThrows(BusinessException.class,()->orders.create(request()));
    }
    @Test void soldOutSecondNightRollsBackFirstNight() {
        db.update("UPDATE tb_room_calendar SET available_stock=0 WHERE calendar_date=?",start.plusDays(1));
        assertThrows(BusinessException.class,()->orders.create(request()));assertEquals(1,stock());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM tb_order",Integer.class));
    }
    @Test void cancellationRacingTimeoutRestoresOnce() throws Exception {
        Order o=orders.create(request());
        race(()->{try{orders.cancel(o.getId(),"test");}catch(BusinessException ignored){} orders.cancelExpired(o);return true;});
        assertEquals(2,stock());assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM tb_order_status_log",Integer.class));
    }
    @Test void merchantCancelRefundsPaymentAndRestoresInventory() {
        Order o=paid();login(2,"MERCHANT");
        Order cancelled=merchants.transitionOrder(o.getId(),"CANCELLED","test");
        assertEquals("REFUNDED",cancelled.getPaymentStatus());assertEquals(2,stock());
    }
    @Test void repeatedFavoriteToggleCanReAddDeletedRecord() {
        assertEquals(List.of(1L),users.toggleFavorite(1L));assertEquals(List.of(),users.toggleFavorite(1L));
        assertEquals(List.of(1L),users.toggleFavorite(1L));
    }
    @Test void disabledAccessTokenIsRejected() throws Exception {
        String token=jwt.createAccessToken(3L,"user");db.update("UPDATE tb_user SET status=0 WHERE id=3");
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/user/profile").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void repeatedFavoritePutIsIdempotent() throws Exception {
        SecurityContextHolder.clearContext();String token=jwt.createAccessToken(3L,"user");
        for(int i=0;i<2;i++) mvc.perform(put("/api/user/favorites/1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_favorite WHERE is_deleted=0",Integer.class));
    }
    @Test void passwordIsNeverSerialized() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+jwt.createAccessToken(1L,"admin")))
            .andExpect(jsonPath("$.data.list[0].password").doesNotExist());
    }
    @Test void duplicateCreateRequestReturnsSameOrderWithoutReservingAgain() {
        var request=request();request.setRequestId(UUID.randomUUID().toString());
        Order first=orders.create(request);Order second=orders.create(request);
        assertEquals(first.getId(),second.getId());assertEquals(0,stock());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_order",Integer.class));
    }
    @Test void failedIdempotentRequestCanBeRetriedAfterRollback() {
        var request=request();request.setRequestId(UUID.randomUUID().toString());
        db.update("UPDATE tb_room_calendar SET available_stock=0 WHERE calendar_date=?",start.plusDays(1));
        assertThrows(BusinessException.class,()->orders.create(request));
        db.update("UPDATE tb_room_calendar SET available_stock=1");
        assertNotNull(orders.create(request).getId());assertEquals(0,stock());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"hotel", "name", "phone", "idCard", "remark"})
    void reusedRequestIdRejectsChangedBookingDetails(String field) {
        var first=request();first.setRequestId(UUID.randomUUID().toString());
        orders.create(first);
        var changed=request();changed.setRequestId(first.getRequestId());
        switch(field) {
            case "hotel" -> changed.setHotelId(999L);
            case "name" -> changed.setGuestName("Other guest");
            case "phone" -> changed.setGuestPhone("13800000004");
            case "idCard" -> changed.setGuestIdCard("other-id");
            case "remark" -> changed.setRemark("different request");
        }
        assertThrows(BusinessException.class,()->orders.create(changed));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_order",Integer.class));
        assertEquals(0,stock());
    }
    @Test void duplicateConcurrentRequestCreatesOnlyOneOrder() throws Exception {
        String key=UUID.randomUUID().toString();
        race(()->{var request=request();request.setRequestId(key);try{orders.create(request);}catch(BusinessException ignored){}return true;});
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_order",Integer.class));assertEquals(0,stock());
    }
    @Test void refundCompletionRestoresUnoccupiedStockExactlyOnce() {
        Order o=paid();orders.refund(o.getId());orders.refund(o.getId());assertEquals(0,stock());
        login(1,"ADMIN");var request=new OrderInterventionRequest();request.setStatus("REFUNDED");
        assertEquals("REFUNDED",orders.intervene(o.getId(),request).getPaymentStatus());
        orders.intervene(o.getId(),request);assertEquals(2,stock());
    }
    @Test void refundOfCompletedStayNeverRestoresConsumedStock() {
        Order o=paid();login(2,"MERCHANT");merchants.transitionOrder(o.getId(),"PENDING_CHECKIN","confirm");
        merchants.transitionOrder(o.getId(),"CHECKED_IN","checkin");
        login(1,"ADMIN");var request=new OrderInterventionRequest();request.setStatus("COMPLETED");orders.intervene(o.getId(),request);
        login(3,"USER");orders.refund(o.getId());login(1,"ADMIN");request.setStatus("REFUNDED");
        orders.intervene(o.getId(),request);assertEquals(0,stock());
    }
    @Test void illegalTransitionLeavesStockPaymentAndLogUnchanged() {
        Order o=orders.create(request());login(1,"ADMIN");var request=new OrderInterventionRequest();request.setStatus("COMPLETED");
        assertThrows(BusinessException.class,()->orders.intervene(o.getId(),request));assertEquals(0,stock());
        assertEquals("UNPAID",db.queryForObject("SELECT payment_status FROM tb_order WHERE id=?",String.class,o.getId()));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_order_status_log",Integer.class));
    }
    @Test void ordersCannotBeReadOrCancelledByOtherUsers() {
        Order o=orders.create(request());login(1,"USER");
        assertThrows(BusinessException.class,()->orders.detail(o.getId()));
        assertThrows(BusinessException.class,()->orders.cancel(o.getId(),"not mine"));assertEquals(0,stock());
    }
    @Test void invalidPageAndUnknownStatusAreRejected() throws Exception {
        SecurityContextHolder.clearContext();String token=jwt.createAccessToken(3L,"user");
        for(String query:List.of("pageNum=0","pageSize=101","status=UNKNOWN"))
            mvc.perform(get("/api/orders?"+query).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.code").value(400));
        for(String endpoint:List.of("/api/merchant/orders","/api/merchant/hotels","/api/admin/users","/api/admin/orders")) {
            boolean admin=endpoint.contains("/admin/");
            String roleToken=jwt.createAccessToken(admin?1L:2L,admin?"admin":"merchant");
            for(String query:List.of("pageNum=0","pageSize=101"))
                mvc.perform(get(endpoint+"?"+query).header("Authorization","Bearer "+roleToken)).andExpect(jsonPath("$.code").value(400));
        }
    }
    @Test void orderViewAndPaginationContainRealBusinessFields() throws Exception {
        Order o=orders.create(request());SecurityContextHolder.clearContext();
        mvc.perform(get("/api/orders?pageSize=1").header("Authorization","Bearer "+jwt.createAccessToken(3L,"user")))
            .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.pages").value(1))
            .andExpect(jsonPath("$.data.list[0].hotel").value("Test Stay"))
            .andExpect(jsonPath("$.data.list[0].guestName").value("Guest"))
            .andExpect(jsonPath("$.data.list[0].totalAmount").value(200))
            .andExpect(jsonPath("$.data.list[0].id").value(o.getId()));
    }
    @Test void concurrentFavoriteAddsAndReAddsRemainUnique() throws Exception {
        race(()->{users.setFavorite(1L,true);return true;});
        login(3,"USER");users.setFavorite(1L,false);users.setFavorite(1L,false);users.setFavorite(1L,true);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM tb_favorite WHERE is_deleted=0",Integer.class));
    }
    @Test void disabledAccountCannotLoginOrRefreshExistingSession() {
        db.update("UPDATE tb_user SET password=? WHERE id=3",encoder.encode("test-password"));
        var login=new com.school.hotel.dto.LoginRequest();login.setPhone("13800000003");login.setPassword("test-password");
        var session=users.login(login);
        var refresh=new com.school.hotel.dto.RefreshTokenRequest();refresh.setRefreshToken((String)session.get("refreshToken"));
        db.update("UPDATE tb_user SET status=0 WHERE id=3");
        assertThrows(BusinessException.class,()->users.login(login));
        assertThrows(BusinessException.class,()->users.refresh(refresh));
    }
    @Test void revokedRoleCannotBeRestoredByOldToken() throws Exception {
        String token=jwt.createAccessToken(1L,"admin");db.update("UPDATE tb_user SET role='user' WHERE id=1");
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.code").value(403));
    }
    @Test void calendarCannotRestoreStockStillReservedByAnOrder() {
        orders.create(request());
        var update=new com.school.hotel.dto.RoomCalendarUpdateRequest();update.setRoomTypeId(1L);
        update.setStartDate(start);update.setEndDate(start.plusDays(1));update.setStock(1);
        update.setPrice(java.math.BigDecimal.valueOf(100));update.setAvailable(true);
        assertThrows(BusinessException.class,()->inventory.updateCalendar(update));assertEquals(0,stock());
    }
    @Test void repeatPaymentAfterMerchantConfirmationRemainsIdempotent() {
        Order o=paid();login(2,"MERCHANT");merchants.transitionOrder(o.getId(),"PENDING_CHECKIN","confirm");
        login(3,"USER");assertEquals("PENDING_CHECKIN",orders.mockPay(o.getId(),true).getOrderStatus());
        assertEquals(0,stock());
    }
}
