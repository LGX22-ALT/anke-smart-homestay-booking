package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.hotel.common.BusinessException;
import com.school.hotel.common.PageResult;
import com.school.hotel.dto.HotelSaveRequest;
import com.school.hotel.dto.HotelSearchRequest;
import java.time.temporal.ChronoUnit;
import com.school.hotel.entity.*;
import com.school.hotel.mapper.*;
import com.school.hotel.utils.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService {
    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomCalendarMapper calendarMapper;
    private final CommentMapper commentMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;

    public HotelService(HotelMapper hotelMapper, RoomTypeMapper roomTypeMapper,
                        RoomCalendarMapper calendarMapper, CommentMapper commentMapper,
                        MerchantMapper merchantMapper, UserMapper userMapper) {
        this.hotelMapper = hotelMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.calendarMapper = calendarMapper;
        this.commentMapper = commentMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
    }

    public PageResult<Hotel> list(HotelSearchRequest filter) {
        boolean dated = filter.getCheckIn() != null || filter.getCheckOut() != null;
        long nights = 0;
        if (dated) {
            if (filter.getCheckIn() == null || filter.getCheckOut() == null
                    || filter.getCheckIn().isBefore(LocalDate.now())
                    || !filter.getCheckOut().isAfter(filter.getCheckIn())) {
                throw new BusinessException(400, "请选择有效的入住和离店日期");
            }
            nights = ChronoUnit.DAYS.between(filter.getCheckIn(), filter.getCheckOut());
            if (nights > 30) throw new BusinessException(400, "单次查询最多支持30晚");
        }
        LambdaQueryWrapper<Hotel> query = new LambdaQueryWrapper<Hotel>()
                .eq(Hotel::getStatus, "ONLINE")
                .and(filter.getKeyword() != null && !filter.getKeyword().isBlank(),
                        value -> value.like(Hotel::getName, filter.getKeyword())
                        .or().like(Hotel::getAddress, filter.getKeyword()).or().like(Hotel::getCity, filter.getKeyword()))
                .eq(filter.getCity() != null && !filter.getCity().isBlank(), Hotel::getCity, filter.getCity())
                .eq(filter.getDistrict() != null && !filter.getDistrict().isBlank(), Hotel::getDistrict, filter.getDistrict())
                .eq(filter.getType() != null && !filter.getType().isBlank(), Hotel::getType, filter.getType())
                .le(filter.getMaxPrice() != null, Hotel::getMinPrice, filter.getMaxPrice())
                .like(filter.getFacility() != null && !filter.getFacility().isBlank(), Hotel::getFacilities, filter.getFacility());
        if (dated || (filter.getGuests() != null && filter.getGuests() > 0)
                || (filter.getBed() != null && !filter.getBed().isBlank())) {
            // Correlate to the already filtered hotel; never materialize all room/calendar rows.
            StringBuilder eligible = new StringBuilder("SELECT 1 FROM tb_room_type r WHERE r.hotel_id=tb_hotel.id AND r.is_deleted=0");
            java.util.ArrayList<Object> params = new java.util.ArrayList<>();
            if(filter.getGuests()!=null && filter.getGuests()>0) {
                eligible.append(" AND r.max_guests >= {").append(params.size()).append("}");params.add(filter.getGuests());
            }
            if(filter.getBed()!=null && !filter.getBed().isBlank()) {
                eligible.append(" AND r.bed LIKE {").append(params.size()).append("}");params.add("%"+filter.getBed()+"%");
            }
            if(dated) {
                int offset=params.size();
                eligible.append(" AND EXISTS (SELECT 1 FROM tb_room_calendar c WHERE c.room_type_id=r.id AND c.is_deleted=0")
                    .append(" AND c.calendar_date >= {").append(offset).append("} AND c.calendar_date < {").append(offset+1).append("}")
                    .append(" AND c.is_available=1 AND c.available_stock>0 GROUP BY c.room_type_id HAVING COUNT(DISTINCT c.calendar_date) = {")
                    .append(offset+2).append("})");
                params.add(filter.getCheckIn());params.add(filter.getCheckOut());params.add(nights);
            }
            query.exists(eligible.toString(),params.toArray());
        }
        switch (filter.getSort() == null ? "score" : filter.getSort()) {
            case "priceAsc" -> query.orderByAsc(Hotel::getMinPrice);
            case "priceDesc" -> query.orderByDesc(Hotel::getMinPrice);
            case "reviews" -> query.orderByDesc(Hotel::getReviewCount);
            default -> query.orderByDesc(Hotel::getScore);
        }
        query.orderByAsc(Hotel::getId);
        Page<Hotel> page = new Page<>(filter.getPageNum(), filter.getPageSize());
        Page<Hotel> result = hotelMapper.selectPage(page, query);
        attachPublicMerchants(result.getRecords());
        return PageResult.of(result.getTotal(), result.getPages(), result.getRecords());
    }

    public Map<String, Object> detail(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || !"ONLINE".equals(hotel.getStatus())) {
            throw new BusinessException(404, "房源不存在或未上线");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("hotel", hotel);
        result.put("rooms", roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>().eq(RoomType::getHotelId, id)));
        result.put("reviews", reviews(id));
        result.put("merchant", publicMerchant(hotel));
        return result;
    }

    public List<RoomCalendar> availability(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        if(checkIn==null || checkOut==null || checkIn.isBefore(LocalDate.now())
            || !checkOut.isAfter(checkIn) || ChronoUnit.DAYS.between(checkIn,checkOut)>30)
            throw new BusinessException(400,"请选择有效日期，单次最多查询30晚");
        Hotel hotel=hotelMapper.selectById(hotelId);
        if(hotel==null || !"ONLINE".equals(hotel.getStatus()))
            throw new BusinessException(404,"房源不存在或未开放");
        List<Long> roomTypeIds = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getHotelId, hotelId)).stream().map(RoomType::getId).toList();
        if (roomTypeIds.isEmpty()) return List.of();
        return calendarMapper.selectList(new LambdaQueryWrapper<RoomCalendar>()
                .in(RoomCalendar::getRoomTypeId, roomTypeIds)
                .ge(RoomCalendar::getCalendarDate, checkIn)
                .lt(RoomCalendar::getCalendarDate, checkOut)
                .orderByAsc(RoomCalendar::getRoomTypeId).orderByAsc(RoomCalendar::getCalendarDate));
    }

    public List<Comment> reviews(Long id) {
        return commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getHotelId, id).eq(Comment::getStatus, "VISIBLE")
                .orderByDesc(Comment::getCreateTime));
    }

    public PageResult<Hotel> merchantHotels(String status, Integer pageNum, Integer pageSize) {
        Merchant merchant = currentMerchant();
        Page<Hotel> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        Page<Hotel> result = hotelMapper.selectPage(page, new LambdaQueryWrapper<Hotel>()
                .eq(Hotel::getMerchantId, merchant.getId())
                .eq(status != null && !status.isBlank(), Hotel::getStatus, status)
                .orderByDesc(Hotel::getCreateTime));
        return PageResult.of(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Transactional
    public Hotel saveMerchantHotel(HotelSaveRequest request, Long id) {
        Merchant merchant = currentMerchant();
        Hotel hotel = id == null ? new Hotel() : hotelMapper.selectById(id);
        if (hotel == null) throw new BusinessException(404, "房源不存在");
        if (id != null && !merchant.getId().equals(hotel.getMerchantId())) {
            throw new BusinessException(403, "无权修改其他商家的房源");
        }
        hotel.setMerchantId(merchant.getId());
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setDistrict(request.getDistrict());
        hotel.setAddress(request.getAddress());
        hotel.setType(request.getType());
        hotel.setCoverImage(request.getCoverImage());
        hotel.setGallery(request.getGallery());
        hotel.setDescription(request.getDescription());
        hotel.setFacilities(request.getFacilities());
        hotel.setNearbyInfo(request.getNearbyInfo());
        hotel.setMinPrice(request.getMinPrice());
        hotel.setStatus("PENDING_AUDIT");
        if (id == null) hotelMapper.insert(hotel); else hotelMapper.updateById(hotel);
        return hotel;
    }

    @Transactional
    public Hotel updateMerchantHotelStatus(Long id, String status) {
        Merchant merchant = currentMerchant();
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || !merchant.getId().equals(hotel.getMerchantId())) {
            throw new BusinessException(403, "无权操作该房源");
        }
        if (!List.of("ONLINE", "OFFLINE", "PENDING_AUDIT").contains(status)) {
            throw new BusinessException("无效的房源状态");
        }
        if ("ONLINE".equals(status) && !"OFFLINE".equals(hotel.getStatus())) {
            throw new BusinessException("未通过审核的房源不能直接上架");
        }
        if ("OFFLINE".equals(status) && !"ONLINE".equals(hotel.getStatus())) {
            throw new BusinessException("只有已上架房源可以下架");
        }
        hotel.setStatus(status);
        hotelMapper.updateById(hotel);
        return hotel;
    }

    private Merchant currentMerchant() {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, AuthContext.userId()));
        if (merchant == null) throw new BusinessException(403, "商家资料不存在");
        return merchant;
    }

    private Map<String, Object> publicMerchant(Hotel hotel) {
        Merchant merchant = merchantMapper.selectById(hotel.getMerchantId());
        if (merchant == null) return Map.of();
        User owner = userMapper.selectById(merchant.getUserId());
        long listingCount = hotelMapper.selectCount(new LambdaQueryWrapper<Hotel>()
                .eq(Hotel::getMerchantId, merchant.getId())
                .eq(Hotel::getStatus, "ONLINE"));
        Map<String, Object> result = new HashMap<>();
        result.put("id", merchant.getId());
        result.put("name", owner == null ? merchant.getContactName() : owner.getName());
        result.put("avatar", owner == null ? null : owner.getAvatar());
        result.put("companyName", merchant.getCompanyName());
        result.put("contactName", merchant.getContactName());
        result.put("contactPhone", owner == null ? "" : maskPhone(owner.getPhone()));
        result.put("status", merchant.getStatus());
        result.put("listingCount", listingCount);
        result.put("joinedAt", merchant.getCreateTime());
        return result;
    }

    private void attachPublicMerchants(List<Hotel> hotels) {
        if (hotels.isEmpty()) return;
        var merchants = merchantMapper.selectBatchIds(hotels.stream().map(Hotel::getMerchantId).distinct().toList());
        if (merchants.isEmpty()) return;
        var owners = userMapper.selectBatchIds(merchants.stream().map(Merchant::getUserId).distinct().toList());
        Map<Long,User> ownersById = new HashMap<>();
        owners.forEach(owner -> ownersById.put(owner.getId(), owner));
        Map<Long,Map<String,Object>> profiles = new HashMap<>();
        for (Merchant merchant : merchants) {
            User owner = ownersById.get(merchant.getUserId());
            Map<String,Object> profile = new HashMap<>();
            profile.put("id", merchant.getId());
            profile.put("companyName", merchant.getCompanyName());
            profile.put("name", owner == null ? merchant.getContactName() : owner.getName());
            profile.put("avatar", owner == null ? null : owner.getAvatar());
            profiles.put(merchant.getId(), profile);
        }
        hotels.forEach(hotel -> hotel.setMerchant(profiles.get(hotel.getMerchantId())));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
