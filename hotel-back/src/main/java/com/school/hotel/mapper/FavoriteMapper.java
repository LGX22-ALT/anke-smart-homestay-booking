package com.school.hotel.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.hotel.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface FavoriteMapper extends BaseMapper<Favorite> {
    @org.apache.ibatis.annotations.Insert("INSERT INTO tb_favorite(user_id,hotel_id,is_deleted) VALUES(#{userId},#{hotelId},0) ON DUPLICATE KEY UPDATE is_deleted=0,update_time=CURRENT_TIMESTAMP")
    int saveFavorite(@org.apache.ibatis.annotations.Param("userId") Long userId,@org.apache.ibatis.annotations.Param("hotelId") Long hotelId);
}
