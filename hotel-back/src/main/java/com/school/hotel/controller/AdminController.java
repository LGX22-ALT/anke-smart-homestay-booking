package com.school.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.hotel.common.PageResult;
import com.school.hotel.common.Result;
import com.school.hotel.dto.AuditRequest;
import com.school.hotel.entity.*;
import com.school.hotel.mapper.*;
import com.school.hotel.utils.AuthContext;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final HotelMapper hotelMapper;
    private final HotelAuditMapper auditMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper logMapper;
    private final CommentMapper commentMapper;
    private final BannerMapper bannerMapper;
    private final SystemDictMapper dictMapper;
    private final PasswordEncoder encoder;
    private final com.school.hotel.service.OrderService orderService;

    public AdminController(UserMapper userMapper, MerchantMapper merchantMapper, HotelMapper hotelMapper,
                           HotelAuditMapper auditMapper, OrderMapper orderMapper, OrderStatusLogMapper logMapper,
                           CommentMapper commentMapper, BannerMapper bannerMapper,
                           SystemDictMapper dictMapper, PasswordEncoder encoder, com.school.hotel.service.OrderService orderService) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.hotelMapper = hotelMapper;
        this.auditMapper = auditMapper;
        this.orderMapper = orderMapper;
        this.logMapper = logMapper;
        this.commentMapper = commentMapper;
        this.bannerMapper = bannerMapper;
        this.dictMapper = dictMapper;
        this.encoder = encoder;
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        BigDecimal amount = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                        .eq(Order::getPaymentStatus, "PAID")
                        .ge(Order::getCreateTime,java.time.LocalDate.now().atStartOfDay())
                        .lt(Order::getCreateTime,java.time.LocalDate.now().plusDays(1).atStartOfDay()))
                .stream().map(Order::getTotalAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.success(Map.of("users", userMapper.selectCount(null),
                "hotels", hotelMapper.selectCount(null), "orders", orderMapper.selectCount(null),
                "todayAmount", amount));
    }

    @GetMapping("/users")
    public Result<?> users(@Valid @ModelAttribute com.school.hotel.dto.PageRequest request) {
        String keyword=request.getKeyword();
        Page<User> page = request.page();
        Page<User> result = userMapper.selectPage(page, new LambdaQueryWrapper<User>()
                .and(keyword != null && !keyword.isBlank(), query -> query.like(User::getName, keyword)
                        .or().like(User::getPhone, keyword)).orderByDesc(User::getCreateTime));
        return Result.success(PageResult.of(result.getTotal(), result.getPages(), result.getRecords()));
    }

    @PutMapping("/users/{id}/status")
    public Result<?> userStatus(@PathVariable Long id, @RequestParam @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.error(404, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success(user);
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.error(404, "用户不存在");
        user.setPassword(encoder.encode("Hotel@123"));
        userMapper.updateById(user);
        return Result.success("密码已重置");
    }

    @GetMapping("/merchants")
    public Result<?> merchants(@RequestParam(required = false) String status) {
        return Result.success(merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                .eq(status != null && !status.isBlank(), Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreateTime)));
    }

    @PutMapping("/merchants/{id}/status")
    public Result<?> merchantStatus(@PathVariable Long id, @RequestParam String status) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) return Result.error(404, "商家不存在");
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);
        return Result.success(merchant);
    }

    @GetMapping("/hotels/audits")
    public Result<?> audits(@RequestParam(required = false) String status) {
        return Result.success(hotelMapper.selectList(new LambdaQueryWrapper<Hotel>()
                .eq(status != null && !status.isBlank(), Hotel::getStatus, status)
                .orderByDesc(Hotel::getUpdateTime)));
    }

    @PutMapping("/hotels/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null) return Result.error(404, "房源不存在");
        boolean approved = "APPROVED".equalsIgnoreCase(request.getResult()) || "通过".equals(request.getResult());
        hotel.setStatus(approved ? "ONLINE" : "REJECTED");
        hotelMapper.updateById(hotel);
        HotelAudit record = new HotelAudit();
        record.setHotelId(id);
        record.setAuditorId(AuthContext.userId());
        record.setResult(approved ? "APPROVED" : "REJECTED");
        record.setReason(request.getReason());
        auditMapper.insert(record);
        return Result.success(hotel);
    }

    @GetMapping("/orders")
    public Result<?> orders(@Valid @ModelAttribute com.school.hotel.dto.OrderPageRequest request) {
        return Result.success(orderService.page(request,null,false));
    }

    @PutMapping("/orders/{id}/intervene")
    public Result<?> intervene(@PathVariable Long id,@Valid @RequestBody com.school.hotel.dto.OrderInterventionRequest request) {
        return Result.success(orderService.view(orderService.intervene(id,request)));
    }

    @GetMapping("/reviews")
    public Result<?> reviews() {
        return Result.success(commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreateTime)));
    }

    @PutMapping("/reviews/{id}")
    public Result<?> moderate(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) return Result.error(404, "评价不存在");
        comment.setStatus(body.getOrDefault("status", "HIDDEN"));
        commentMapper.updateById(comment);
        return Result.success(comment);
    }

    @PutMapping("/reviews/{id}/user-status")
    public Result<?> reviewUserStatus(@PathVariable Long id, @RequestParam @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) Integer status) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) return Result.error(404, "评价不存在");
        User user = userMapper.selectById(comment.getUserId());
        if (user == null) return Result.error(404, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success(user);
    }

    @GetMapping("/banners") public Result<?> banners(){return Result.success(bannerMapper.selectList(new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortNo)));}
    @PostMapping("/banners") public Result<?> createBanner(@RequestBody Banner banner){bannerMapper.insert(banner);return Result.success(banner);}
    @PutMapping("/banners/{id}") public Result<?> updateBanner(@PathVariable Long id,@RequestBody Banner banner){banner.setId(id);bannerMapper.updateById(banner);return Result.success(bannerMapper.selectById(id));}
    @DeleteMapping("/banners/{id}") public Result<?> deleteBanner(@PathVariable Long id){bannerMapper.deleteById(id);return Result.success(null);}
    @GetMapping("/system-dicts") public Result<?> dicts(@RequestParam(required=false) String dictType){return Result.success(dictMapper.selectList(new LambdaQueryWrapper<SystemDict>().eq(dictType!=null&&!dictType.isBlank(),SystemDict::getDictType,dictType).orderByAsc(SystemDict::getSortNo)));}
    @PostMapping("/system-dicts") public Result<?> createDict(@RequestBody SystemDict dict){dictMapper.insert(dict);return Result.success(dict);}
    @PutMapping("/system-dicts/{id}") public Result<?> updateDict(@PathVariable Long id,@RequestBody SystemDict dict){dict.setId(id);dictMapper.updateById(dict);return Result.success(dictMapper.selectById(id));}
    @DeleteMapping("/system-dicts/{id}") public Result<?> deleteDict(@PathVariable Long id){dictMapper.deleteById(id);return Result.success(null);}
}
