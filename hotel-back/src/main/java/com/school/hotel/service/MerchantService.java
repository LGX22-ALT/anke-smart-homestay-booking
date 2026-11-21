package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.common.BusinessException;
import com.school.hotel.dto.ReplyRequest;
import com.school.hotel.entity.*;
import com.school.hotel.mapper.*;
import com.school.hotel.utils.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MerchantService {
    private final MerchantMapper merchantMapper;
    private final HotelMapper hotelMapper;
    private final OrderMapper orderMapper;
    private final CommentMapper commentMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomCalendarMapper calendarMapper;
    private final OrderService orderService;

    public MerchantService(MerchantMapper merchantMapper, HotelMapper hotelMapper,
                           OrderMapper orderMapper, CommentMapper commentMapper,
                           RoomTypeMapper roomTypeMapper, RoomCalendarMapper calendarMapper,
                           OrderService orderService) {
        this.merchantMapper = merchantMapper;
        this.hotelMapper = hotelMapper;
        this.orderMapper = orderMapper;
        this.commentMapper = commentMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.calendarMapper = calendarMapper;
        this.orderService = orderService;
    }

    public Merchant merchant() {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, AuthContext.userId()));
        if (merchant == null) throw new BusinessException(403, "商家资料不存在");
        return merchant;
    }

    public Map<String, Object> dashboard() {
        List<Order> orders = orders(null);
        return Map.of("todayOrders", orders.size(),
                "revenue", orders.stream().filter(order -> "PAID".equals(order.getPaymentStatus())).map(Order::getTotalAmount).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                "pendingOrders", orders.stream().filter(order -> "PENDING_CHECKIN".equals(order.getOrderStatus())).count());
    }

    public List<Order> orders(String status) {
        List<Long> hotelIds = hotelIds();
        if (hotelIds.isEmpty()) return List.of();
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getHotelId, hotelIds)
                .eq(status != null && !status.isBlank(), Order::getOrderStatus, status)
                .orderByDesc(Order::getCreateTime));
    }

    public List<Comment> reviews() {
        List<Long> hotelIds = hotelIds();
        if (hotelIds.isEmpty()) return List.of();
        return commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .in(Comment::getHotelId, hotelIds).orderByDesc(Comment::getCreateTime));
    }

    @Transactional
    public Order transitionOrder(Long id, String target, String remark) {
        return orderService.merchantTransition(id,target,remark,hotelIds());
    }

    public com.school.hotel.common.PageResult<com.school.hotel.vo.OrderView> orderPage(com.school.hotel.dto.OrderPageRequest request) {
        return orderService.page(request,hotelIds(),false);
    }

    public Comment reply(Long id, ReplyRequest request) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null || !hotelIds().contains(comment.getHotelId())) {
            throw new BusinessException(404, "评价不存在");
        }
        comment.setMerchantReply(request.getReply());
        commentMapper.updateById(comment);
        return comment;
    }

    public void assertOwnsRoomType(Long roomTypeId) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        if (roomType == null || !hotelIds().contains(roomType.getHotelId())) {
            throw new BusinessException(403, "无权操作该房型");
        }
    }

    public List<RoomType> roomTypes(Long hotelId) {
        if(!hotelIds().contains(hotelId)) throw new BusinessException(403,"无权操作该房源");
        return roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>().eq(RoomType::getHotelId,hotelId));
    }

    private List<Long> hotelIds() {
        Long merchantId = merchant().getId();
        return hotelMapper.selectList(new LambdaQueryWrapper<Hotel>().eq(Hotel::getMerchantId, merchantId))
                .stream().map(Hotel::getId).toList();
    }

}
