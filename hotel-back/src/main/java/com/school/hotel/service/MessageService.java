package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.common.BusinessException;
import com.school.hotel.entity.Hotel;
import com.school.hotel.entity.Merchant;
import com.school.hotel.entity.PrivateMessage;
import com.school.hotel.entity.User;
import com.school.hotel.mapper.HotelMapper;
import com.school.hotel.mapper.MerchantMapper;
import com.school.hotel.mapper.PrivateMessageMapper;
import com.school.hotel.mapper.UserMapper;
import com.school.hotel.utils.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {
    private static final String USER = "USER";
    private static final String MERCHANT = "MERCHANT";

    private final PrivateMessageMapper messageMapper;
    private final HotelMapper hotelMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;

    public MessageService(PrivateMessageMapper messageMapper, HotelMapper hotelMapper,
                          MerchantMapper merchantMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.hotelMapper = hotelMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public List<Map<String, Object>> userThread(Long hotelId) {
        Long userId = AuthContext.userId();
        Hotel hotel = onlineHotel(hotelId);
        List<PrivateMessage> messages = thread(hotelId, userId, hotel.getMerchantId());
        markRead(messages, MERCHANT);
        return messages.stream().map(message -> messageView(message, userId)).toList();
    }

    @Transactional
    public Map<String, Object> sendFromUser(Long hotelId, String content) {
        Long userId = AuthContext.userId();
        Hotel hotel = onlineHotel(hotelId);
        PrivateMessage message = newMessage(hotelId, userId, hotel.getMerchantId(), userId, USER, content);
        messageMapper.insert(message);
        return messageView(message, userId);
    }

    public List<Map<String,Object>> userConversations() {
        Long userId = AuthContext.userId();
        List<PrivateMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getUserId, userId)
                .orderByDesc(PrivateMessage::getCreateTime).orderByDesc(PrivateMessage::getId));
        Map<String,List<PrivateMessage>> grouped = new LinkedHashMap<>();
        for (PrivateMessage message : messages) {
            grouped.computeIfAbsent(message.getHotelId()+":"+message.getMerchantId(), key -> new ArrayList<>()).add(message);
        }
        return grouped.values().stream().map(group -> {
            PrivateMessage latest = group.get(0);
            Hotel hotel = hotelMapper.selectById(latest.getHotelId());
            Merchant merchant = merchantMapper.selectById(latest.getMerchantId());
            User owner = merchant == null ? null : userMapper.selectById(merchant.getUserId());
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("hotelId", latest.getHotelId());
            result.put("hotelName", hotel == null ? "已下架房源" : hotel.getName());
            result.put("hotelImage", hotel == null ? null : hotel.getCoverImage());
            result.put("merchantName", merchant == null ? "民宿商家" : merchant.getCompanyName());
            result.put("merchantAvatar", owner == null ? null : owner.getAvatar());
            result.put("latestMessage", latest.getContent());
            result.put("latestTime", latest.getCreateTime());
            result.put("unavailable", hotel == null || !"ONLINE".equals(hotel.getStatus())
                    || !latest.getMerchantId().equals(hotel.getMerchantId()));
            result.put("unreadCount", group.stream().filter(message -> MERCHANT.equals(message.getSenderRole())
                    && Integer.valueOf(0).equals(message.getIsRead())).count());
            return result;
        }).toList();
    }

    public List<Map<String, Object>> merchantConversations() {
        Merchant merchant = currentMerchant();
        List<PrivateMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getMerchantId, merchant.getId())
                .orderByDesc(PrivateMessage::getCreateTime).orderByDesc(PrivateMessage::getId));
        Map<String, List<PrivateMessage>> grouped = new LinkedHashMap<>();
        for (PrivateMessage message : messages) {
            grouped.computeIfAbsent(message.getHotelId() + ":" + message.getUserId(), key -> new ArrayList<>())
                    .add(message);
        }
        return grouped.values().stream().map(this::conversationView)
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("latestTime")), Comparator.reverseOrder()))
                .toList();
    }

    @Transactional
    public List<Map<String, Object>> merchantThread(Long hotelId, Long userId) {
        Merchant merchant = assertOwnsHotel(hotelId);
        List<PrivateMessage> messages = thread(hotelId, userId, merchant.getId());
        markRead(messages, USER);
        return messages.stream().map(message -> messageView(message, merchant.getUserId())).toList();
    }

    @Transactional
    public Map<String, Object> sendFromMerchant(Long hotelId, Long userId, String content) {
        Merchant merchant = assertOwnsHotel(hotelId);
        if (userMapper.selectById(userId) == null) throw new BusinessException(404, "住客不存在");
        PrivateMessage message = newMessage(hotelId, userId, merchant.getId(), merchant.getUserId(), MERCHANT, content);
        messageMapper.insert(message);
        return messageView(message, merchant.getUserId());
    }

    private List<PrivateMessage> thread(Long hotelId, Long userId, Long merchantId) {
        return messageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getHotelId, hotelId)
                .eq(PrivateMessage::getUserId, userId)
                .eq(PrivateMessage::getMerchantId, merchantId)
                .orderByAsc(PrivateMessage::getCreateTime).orderByAsc(PrivateMessage::getId));
    }

    private PrivateMessage newMessage(Long hotelId, Long userId, Long merchantId,
                                      Long senderUserId, String senderRole, String content) {
        PrivateMessage message = new PrivateMessage();
        message.setHotelId(hotelId);
        message.setUserId(userId);
        message.setMerchantId(merchantId);
        message.setSenderUserId(senderUserId);
        message.setSenderRole(senderRole);
        message.setContent(content.trim());
        message.setIsRead(0);
        return message;
    }

    private void markRead(List<PrivateMessage> messages, String senderRole) {
        messages.stream()
                .filter(message -> senderRole.equals(message.getSenderRole()) && Integer.valueOf(0).equals(message.getIsRead()))
                .forEach(message -> {
                    message.setIsRead(1);
                    messageMapper.updateById(message);
                });
    }

    private Map<String, Object> messageView(PrivateMessage message, Long viewerUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", message.getId());
        result.put("content", message.getContent());
        result.put("senderRole", message.getSenderRole());
        result.put("mine", viewerUserId.equals(message.getSenderUserId()));
        result.put("read", Integer.valueOf(1).equals(message.getIsRead()));
        result.put("createTime", message.getCreateTime());
        return result;
    }

    private Map<String, Object> conversationView(List<PrivateMessage> messages) {
        PrivateMessage latest = messages.get(0);
        Hotel hotel = hotelMapper.selectById(latest.getHotelId());
        User user = userMapper.selectById(latest.getUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hotelId", latest.getHotelId());
        result.put("hotelName", hotel == null ? "已下架房源" : hotel.getName());
        result.put("userId", latest.getUserId());
        result.put("userName", user == null ? "住客" : user.getName());
        result.put("userAvatar", user == null ? null : user.getAvatar());
        result.put("latestMessage", latest.getContent());
        result.put("latestTime", latest.getCreateTime());
        result.put("unreadCount", messages.stream()
                .filter(message -> USER.equals(message.getSenderRole()) && Integer.valueOf(0).equals(message.getIsRead()))
                .count());
        return result;
    }

    private Hotel onlineHotel(Long hotelId) {
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || !"ONLINE".equals(hotel.getStatus())) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        if (merchantMapper.selectById(hotel.getMerchantId()) == null) {
            throw new BusinessException(404, "房源商家不存在");
        }
        return hotel;
    }

    private Merchant currentMerchant() {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, AuthContext.userId()));
        if (merchant == null) throw new BusinessException(403, "商家资料不存在");
        return merchant;
    }

    private Merchant assertOwnsHotel(Long hotelId) {
        Merchant merchant = currentMerchant();
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || !merchant.getId().equals(hotel.getMerchantId())) {
            throw new BusinessException(403, "无权查看该房源的消息");
        }
        return merchant;
    }
}
