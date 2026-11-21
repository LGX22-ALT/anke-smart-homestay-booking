package com.school.hotel.config;

import com.school.hotel.common.Result;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityErrorController implements ErrorController {
    @RequestMapping("/error")
    public Result<Void> error(jakarta.servlet.http.HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        int code = status == null ? 500 : status;
        return Result.error(code, code == 401 ? "请先登录" : code == 403 ? "无权访问" : "请求失败");
    }
}
