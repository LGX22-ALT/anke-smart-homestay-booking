package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.common.*;
import com.school.hotel.config.TokenStore;
import com.school.hotel.dto.*;
import com.school.hotel.entity.*;
import com.school.hotel.mapper.*;
import com.school.hotel.utils.AuthContext;
import com.school.hotel.vo.OrderView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Set<String> CANCELLABLE=Set.of("PENDING_PAYMENT","PAID","PENDING_CHECKIN","CONFIRMED");
    private final OrderMapper orders;
    private final OrderGuestMapper guests;
    private final OrderStatusLogMapper logs;
    private final HotelMapper hotels;
    private final RoomTypeMapper roomTypes;
    private final RoomInventoryService inventory;
    private final TokenStore tokens;
    public OrderService(OrderMapper orders,OrderGuestMapper guests,OrderStatusLogMapper logs,
                        HotelMapper hotels,RoomTypeMapper roomTypes,RoomInventoryService inventory,TokenStore tokens){
        this.orders=orders;this.guests=guests;this.logs=logs;this.hotels=hotels;this.roomTypes=roomTypes;this.inventory=inventory;this.tokens=tokens;
    }
    @Transactional
    public Order create(OrderCreateRequest r){
        Long uid=AuthContext.userId();
        if(r.getCheckIn()==null || r.getCheckOut()==null || r.getCheckIn().isBefore(LocalDate.now())
            || !r.getCheckIn().isBefore(r.getCheckOut()) || ChronoUnit.DAYS.between(r.getCheckIn(),r.getCheckOut())>30)
            throw new BusinessException("入住日期无效，最多连续预订30晚");
        String key=r.getRequestId()==null || r.getRequestId().isBlank()?null:"order:idempotency:"+uid+":"+r.getRequestId();
        if(key!=null){
            String value=tokens.get(key);
            if(value!=null && !"PROCESSING".equals(value)){
                Order existing=orders.selectById(Long.valueOf(value));
                if(existing!=null && uid.equals(existing.getUserId())){
                    OrderGuest guest=guests.selectOne(new LambdaQueryWrapper<OrderGuest>()
                        .eq(OrderGuest::getOrderId,existing.getId()).orderByAsc(OrderGuest::getId).last("LIMIT 1"));
                    if(!Objects.equals(existing.getHotelId(),r.getHotelId())
                        || !Objects.equals(existing.getRoomTypeId(),r.getRoomTypeId())
                        || !Objects.equals(existing.getCheckIn(),r.getCheckIn())
                        || !Objects.equals(existing.getCheckOut(),r.getCheckOut())
                        || !Objects.equals(existing.getGuests(),r.getGuests())
                        || !Objects.equals(existing.getRemark(),r.getRemark()) || guest==null
                        || !Objects.equals(guest.getName(),r.getGuestName())
                        || !Objects.equals(guest.getPhone(),r.getGuestPhone())
                        || !Objects.equals(guest.getIdCard(),r.getGuestIdCard()))
                        throw new BusinessException("请求编号已用于其他预订");
                    return existing;
                }
            }
            if(!tokens.putIfAbsent(key,"PROCESSING",Duration.ofMinutes(30))) throw new BusinessException(1001,"订单正在处理，请勿重复提交");
        }
        final Order[] created=new Order[1];
        if(key!=null) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){
                if(status==STATUS_COMMITTED && created[0]!=null) tokens.save(key,String.valueOf(created[0].getId()),Duration.ofMinutes(30));
                else tokens.delete(key);
            }
        });
        Hotel hotel=hotels.selectById(r.getHotelId());
        if(hotel==null || !"ONLINE".equals(hotel.getStatus())) throw new BusinessException("房源不可预订");
        BigDecimal amount=inventory.reserve(r);
        Order order=new Order();order.setOrderNo("HO"+UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(uid);order.setHotelId(r.getHotelId());order.setRoomTypeId(r.getRoomTypeId());
        order.setCheckIn(r.getCheckIn());order.setCheckOut(r.getCheckOut());
        order.setNights((int)ChronoUnit.DAYS.between(r.getCheckIn(),r.getCheckOut()));order.setGuests(r.getGuests());
        order.setTotalAmount(amount);order.setOrderStatus("PENDING_PAYMENT");order.setPaymentStatus("UNPAID");order.setRemark(r.getRemark());
        orders.insert(order);
        OrderGuest guest=new OrderGuest();guest.setOrderId(order.getId());guest.setName(r.getGuestName());
        guest.setPhone(r.getGuestPhone());guest.setIdCard(r.getGuestIdCard());guests.insert(guest);
        log(order,null,"PENDING_PAYMENT","创建订单");created[0]=order;return order;
    }
    public PageResult<OrderView> page(OrderPageRequest request,List<Long> hotelIds,boolean own){
        if(hotelIds!=null && hotelIds.isEmpty()) return PageResult.of(0,0,List.of());
        var query=new LambdaQueryWrapper<Order>()
            .eq(own,Order::getUserId,AuthContext.userId())
            .in(hotelIds!=null,Order::getHotelId,hotelIds==null?List.of():hotelIds)
            .eq(request.getStatus()!=null,Order::getOrderStatus,request.getStatus())
            .like(request.getKeyword()!=null && !request.getKeyword().isBlank(),Order::getOrderNo,request.getKeyword())
            .orderByDesc(Order::getCreateTime).orderByDesc(Order::getId);
        var result=orders.selectPage(request.<Order>page(),query);
        return PageResult.of(result.getTotal(),result.getPages(),views(result.getRecords()));
    }
    public Order detail(Long id){return owned(id,false);}
    private Order owned(Long id,boolean lock){
        var query=new LambdaQueryWrapper<Order>().eq(Order::getId,id).eq(Order::getUserId,AuthContext.userId());
        if(lock)query.last("FOR UPDATE");
        Order order=orders.selectOne(query);if(order==null)throw new BusinessException(404,"订单不存在");return order;
    }
    private Order lock(Long id){
        Order order=orders.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getId,id).last("FOR UPDATE"));
        if(order==null)throw new BusinessException(404,"订单不存在");return order;
    }
    @Transactional public Order cancel(Long id,String reason){return cancelLocked(owned(id,true),reason,false);}
    private Order cancelLocked(Order order,String reason,boolean expired){
        if("CANCELLED".equals(order.getOrderStatus()))return order;
        String from=order.getOrderStatus();
        if(!CANCELLABLE.contains(from))throw new BusinessException("当前订单不可取消");
        order.setPaymentStatus("PENDING_PAYMENT".equals(from)?(expired?"CLOSED":"UNPAID"):"REFUNDED");
        order.setCancelReason(reason);inventory.release(order);return change(order,"CANCELLED",reason);
    }
    @Transactional public Order mockPay(Long id,boolean success){
        Order order=owned(id,true);
        if("PAID".equals(order.getPaymentStatus()) && Set.of("PAID","PENDING_CHECKIN","CONFIRMED","CHECKED_IN","COMPLETED").contains(order.getOrderStatus()))return order;
        if(!"PENDING_PAYMENT".equals(order.getOrderStatus()))throw new BusinessException("订单不在待支付状态");
        if(order.getCreateTime()!=null && order.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(30)))
            return cancelLocked(order,"支付超时自动取消",true);
        if(!success)return order;
        order.setPaymentStatus("PAID");return change(order,"PAID","模拟支付成功");
    }
    @Transactional public Order refund(Long id){
        Order order=owned(id,true);String from=order.getOrderStatus();
        if(Set.of("REFUNDING","REFUNDED").contains(from))return order;
        if(!Set.of("PAID","PENDING_CHECKIN","CONFIRMED","COMPLETED").contains(from))
            throw new BusinessException("当前订单不可申请退款");
        order.setPaymentStatus("REFUNDING");return change(order,"REFUNDING","用户申请退款");
    }
    @Transactional public void cancelExpired(Order candidate){
        Order order=lock(candidate.getId());
        if("PENDING_PAYMENT".equals(order.getOrderStatus()))cancelLocked(order,"支付超时自动取消",true);
    }
    @Transactional public Order merchantTransition(Long id,String target,String remark,List<Long> hotelIds){
        Order order=lock(id);
        if(!hotelIds.contains(order.getHotelId()))throw new BusinessException(404,"订单不存在");
        if("CANCELLED".equals(target))return cancelLocked(order,remark,false);
        if(target.equals(order.getOrderStatus()))return order;
        boolean valid="PENDING_CHECKIN".equals(target) && "PAID".equals(order.getOrderStatus())
            || "CHECKED_IN".equals(target) && Set.of("PENDING_CHECKIN","CONFIRMED").contains(order.getOrderStatus());
        if(!valid)throw new BusinessException("当前订单状态不允许该操作");return change(order,target,remark);
    }
    @Transactional public Order intervene(Long id,OrderInterventionRequest request){
        Order order=lock(id);String target=request.getStatus();
        if(target.equals(order.getOrderStatus()))return order;
        if("CANCELLED".equals(target))return cancelLocked(order,request.getRemark(),false);
        if("REFUNDED".equals(target) && "REFUNDING".equals(order.getOrderStatus())){
            OrderStatusLog original=logs.selectOne(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId,id).eq(OrderStatusLog::getToStatus,"REFUNDING").orderByDesc(OrderStatusLog::getId).last("LIMIT 1"));
            if(original==null)throw new BusinessException("缺少退款申请记录");
            if(!"COMPLETED".equals(original.getFromStatus()))inventory.release(order);
            order.setPaymentStatus("REFUNDED");return change(order,target,request.getRemark());
        }
        if("COMPLETED".equals(target) && "CHECKED_IN".equals(order.getOrderStatus()))return change(order,target,request.getRemark());
        throw new BusinessException("不允许的订单状态流转");
    }
    private Order change(Order order,String target,String remark){
        String from=order.getOrderStatus();order.setOrderStatus(target);orders.updateById(order);log(order,from,target,remark);return order;
    }
    private void log(Order order,String from,String target,String remark){
        OrderStatusLog log=new OrderStatusLog();log.setOrderId(order.getId());log.setFromStatus(from);log.setToStatus(target);
        log.setOperatorId(AuthContext.hasRole("USER")||AuthContext.hasRole("MERCHANT")||AuthContext.hasRole("ADMIN")?AuthContext.userId():null);
        log.setRemark(remark);logs.insert(log);
    }
    public OrderView view(Order order){return views(List.of(order)).get(0);}
    private List<OrderView> views(List<Order> rows){
        if(rows.isEmpty())return List.of();
        var hotelMap=hotels.selectBatchIds(rows.stream().map(Order::getHotelId).distinct().toList())
            .stream().collect(Collectors.toMap(Hotel::getId,h->h));
        var roomMap=roomTypes.selectBatchIds(rows.stream().map(Order::getRoomTypeId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(RoomType::getId,r->r));
        var orderGuests=guests.selectList(new LambdaQueryWrapper<OrderGuest>().in(OrderGuest::getOrderId,rows.stream().map(Order::getId).toList()))
            .stream().collect(Collectors.toMap(OrderGuest::getOrderId,g->g,(a,b)->a));
        return rows.stream().map(o->OrderView.of(o,hotelMap.get(o.getHotelId()),roomMap.get(o.getRoomTypeId()),orderGuests.get(o.getId()))).toList();
    }
}

