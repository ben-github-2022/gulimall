package com.atguigu.gulimall.authserver.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.authserver.feign.ThirdPartyFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {

    @Autowired
    ThirdPartyFeignService feignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@PathVariable("phone") String phone){
        //TODO 接口防刷

        // 再次校验，存入redis
        String keys = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        long l = Long.parseLong(keys.split("_")[1]);
        if(System.currentTimeMillis()-l <60000){
            //60s内不能重发
            return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
        }


        String substring = UUID.randomUUID().toString().substring(0, 5);
        String code="**code**:"+substring+",**minute**:5";
        String redisCode=substring+"_"+System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,redisCode,
                10, TimeUnit.MINUTES);


        // c12345,**minute**:5"
        feignService.sendCode(phone,code);

        return R.ok();
    }

    @GetMapping("/login.html")
    public String loginPage() {

        return "login";
    }

    @GetMapping("/reg.html")
    public String registerPage() {

        return "reg";
    }
}
