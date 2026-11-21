package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.common.BusinessException;
import com.school.hotel.common.enums.UserRole;
import com.school.hotel.config.JwtProperties;
import com.school.hotel.config.JwtTokenService;
import com.school.hotel.config.TokenStore;
import com.school.hotel.dto.CommentSaveRequest;
import com.school.hotel.dto.LoginRequest;
import com.school.hotel.dto.ProfileUpdateRequest;
import com.school.hotel.dto.RefreshTokenRequest;
import com.school.hotel.dto.RegisterRequest;
import com.school.hotel.entity.Comment;
import com.school.hotel.entity.Favorite;
import com.school.hotel.entity.Order;
import com.school.hotel.entity.User;
import com.school.hotel.mapper.CommentMapper;
import com.school.hotel.mapper.FavoriteMapper;
import com.school.hotel.mapper.OrderMapper;
import com.school.hotel.mapper.UserMapper;
import com.school.hotel.utils.AuthContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;
    private final OrderMapper orderMapper;
    private final PasswordEncoder encoder;
    private final JwtTokenService jwt;
    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;
    private final AvatarStorageService avatars;

    public UserService(UserMapper userMapper, FavoriteMapper favoriteMapper, CommentMapper commentMapper,
                       OrderMapper orderMapper, PasswordEncoder encoder, JwtTokenService jwt,
                       JwtProperties jwtProperties, TokenStore tokenStore, AvatarStorageService avatars) {
        this.userMapper = userMapper;
        this.favoriteMapper = favoriteMapper;
        this.commentMapper = commentMapper;
        this.orderMapper = orderMapper;
        this.encoder = encoder;
        this.jwt = jwt;
        this.jwtProperties = jwtProperties;
        this.tokenStore = tokenStore;
        this.avatars = avatars;
    }

    public Map<String, Object> login(LoginRequest request) {
        String account = request.getPhone().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, account));
        if (user == null || !encoder.matches(request.getPassword(), user.getPassword())
                || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(401, "账号或密码错误");
        }
        return tokens(user);
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String phone = request.getPhone().trim();
        String email = request.getEmail().trim().toLowerCase();
        long duplicateCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .or(wrapper -> wrapper.eq(User::getEmail, email)));
        if (duplicateCount > 0) throw new BusinessException(400, "手机号或邮箱已注册");

        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(encoder.encode(request.getPassword()));
        user.setName(request.getUsername().trim());
        user.setRole(UserRole.USER.name().toLowerCase());
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(400, "手机号或邮箱已注册");
        }
        return tokens(user);
    }

    public Map<String, Object> refresh(RefreshTokenRequest request) {
        try {
            var claims = jwt.parse(request.getRefreshToken());
            if (!"refresh".equals(String.valueOf(claims.get("type")))) {
                throw new BusinessException(401, "刷新令牌类型无效");
            }
            String key = "refresh:" + request.getRefreshToken();
            String value = tokenStore.get(key);
            if (value == null || !value.equals(claims.getSubject())) {
                throw new BusinessException(401, "刷新令牌已失效");
            }
            User user = userMapper.selectById(Long.valueOf(claims.getSubject()));
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                throw new BusinessException(401, "用户不可用");
            }
            return tokens(user,key);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) tokenStore.delete("refresh:" + refreshToken);
    }

    public Map<String, Object> profile() { return profile(userMapper.selectById(AuthContext.userId())); }

    public String uploadAvatar(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        return avatars.store(file);
    }

    @Transactional
    public Map<String, Object> updateProfile(ProfileUpdateRequest request) {
        User user = userMapper.selectById(AuthContext.userId());
        if (user == null) throw new BusinessException(404, "用户不存在");
        User changes = new User();
        changes.setId(user.getId());
        String phone = request.getPhone() == null ? user.getPhone() : request.getPhone().trim();
        String email = request.getEmail() == null ? user.getEmail() : request.getEmail().trim().toLowerCase();
        LambdaQueryWrapper<User> duplicate = new LambdaQueryWrapper<User>()
                .ne(User::getId, user.getId()).and(wrapper -> wrapper.eq(User::getPhone, phone).or()
                        .eq(User::getEmail, email));
        if (userMapper.selectCount(duplicate) > 0) throw new BusinessException(400, "手机号或邮箱已被使用");
        if(request.getName()!=null) {
            String name=request.getName().trim();
            if(name.isBlank() || name.length()>64) throw new BusinessException(400,"个人名称需为 1 至 64 个字符");
            changes.setName(name);
        }
        if(request.getPhone()!=null) changes.setPhone(phone);
        if(request.getEmail()!=null) changes.setEmail(email);
        if(request.getAvatar()!=null) {
            if(!request.getAvatar().equals(user.getAvatar())) avatars.validateReference(request.getAvatar());
            changes.setAvatar(request.getAvatar());
        }
        if(request.getIdCard()!=null) changes.setIdCard(request.getIdCard());
        if(request.getName()!=null || request.getPhone()!=null || request.getEmail()!=null
                || request.getAvatar()!=null || request.getIdCard()!=null) userMapper.updateById(changes);
        return profile(userMapper.selectById(user.getId()));
    }

    public List<Long> favorites() {
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, AuthContext.userId()))
                .stream().map(Favorite::getHotelId).toList();
    }

    @Transactional
    public List<Long> toggleFavorite(Long hotelId) {
        Long uid = AuthContext.userId();
        lockFavoriteUser(uid);
        Favorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, uid)
                .eq(Favorite::getHotelId, hotelId));
        if (favorite == null) {
            favoriteMapper.saveFavorite(uid,hotelId);
        } else favoriteMapper.deleteById(favorite.getId());
        return favorites();
    }

    private void lockFavoriteUser(Long uid) {
        User user=userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId,uid).last("FOR UPDATE"));
        if(user==null || !Integer.valueOf(1).equals(user.getStatus()))throw new BusinessException(401,"账号不可用");
    }

    @Transactional
    public List<Long> setFavorite(Long hotelId,boolean enabled) {
        Long uid=AuthContext.userId();lockFavoriteUser(uid);
        if(enabled)favoriteMapper.saveFavorite(uid,hotelId);
        else favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId,uid).eq(Favorite::getHotelId,hotelId));
        return favorites();
    }

    public List<Comment> comments() {
        return commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, AuthContext.userId())
                .orderByDesc(Comment::getCreateTime));
    }

    @Transactional
    public Comment createComment(CommentSaveRequest request) {
        Long uid = AuthContext.userId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getId, request.getOrderId())
                .eq(Order::getUserId, uid));
        if (order == null || !"COMPLETED".equals(order.getOrderStatus())) throw new BusinessException("仅完成订单可以评价");
        if (commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getOrderId, order.getId())) > 0)
            throw new BusinessException("该订单已经评价");
        Comment comment = new Comment(); comment.setOrderId(order.getId()); comment.setHotelId(order.getHotelId());
        comment.setUserId(uid); comment.setScore(request.getScore()); comment.setContent(request.getContent());
        comment.setImages(request.getImages()); comment.setStatus("VISIBLE"); commentMapper.insert(comment); return comment;
    }

    @Transactional
    public Comment updateComment(Long id, CommentSaveRequest request) {
        Comment comment = ownedComment(id); comment.setScore(request.getScore()); comment.setContent(request.getContent());
        comment.setImages(request.getImages()); commentMapper.updateById(comment); return comment;
    }

    @Transactional public void deleteComment(Long id) { commentMapper.deleteById(ownedComment(id).getId()); }

    private Comment ownedComment(Long id) {
        Comment comment = commentMapper.selectOne(new LambdaQueryWrapper<Comment>().eq(Comment::getId, id)
                .eq(Comment::getUserId, AuthContext.userId()));
        if (comment == null) throw new BusinessException(404, "评论不存在"); return comment;
    }

    private Map<String, Object> tokens(User user) {
        return tokens(user,null);
    }
    private Map<String, Object> tokens(User user,String previousKey) {
        String access = jwt.createAccessToken(user.getId(), user.getRole());
        String refresh = jwt.createRefreshToken(user.getId(), user.getRole());
        Duration ttl=Duration.ofDays(jwtProperties.getRefreshExpireDays());
        if(previousKey==null)tokenStore.save("refresh:" + refresh, String.valueOf(user.getId()),ttl);
        else if(!tokenStore.rotate(previousKey,"refresh:"+refresh,String.valueOf(user.getId()),ttl))
            throw new BusinessException(401,"刷新令牌已失效");
        Map<String, Object> result = new HashMap<>(); result.put("token", access); result.put("refreshToken", refresh);
        result.put("expiresIn", jwtProperties.getAccessExpireMinutes() * 60); result.put("role", user.getRole());
        result.put("userInfo", profile(user)); return result;
    }

    private Map<String, Object> profile(User user) {
        if (user == null) throw new BusinessException(404, "用户不存在");
        Map<String, Object> map = new HashMap<>(); map.put("id", user.getId()); map.put("name", user.getName());
        map.put("username", user.getName()); map.put("phone", user.getPhone()); map.put("email", user.getEmail());
        map.put("avatar", user.getAvatar()); map.put("role", user.getRole());
        map.put("verified", user.getIdCard() != null && !user.getIdCard().isBlank()); return map;
    }
}
