package com.atguigu.gulimall.authserver.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.authserver.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.authserver.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartyFeignService feignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @GetMapping("/sms/sendcode")
    public R sendCode(@PathVariable("phone") String phone){
        System.out.println("phone= "+phone);

        System.out.println("被调用。，，");
        //TODO 接口防刷

        // 再次校验，存入redis
        String keys = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        long l = Long.parseLong(keys.split("_")[1]);
        if(System.currentTimeMillis()-l <60000){
            //60s内不能重发
            return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
        }


        String substring = UUID.randomUUID().toString().substring(0, 5);
        StringBuilder stringBuilder=new StringBuilder("**code**:");
      //  String code = stringBuilder.append(substring).append(",**minute**:5").toString();
        //  String code="**code**:"+substring+",**minute**:5";
          String code="**code**:6666,**minute**:5";
        String redisCode=substring+"_"+System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,redisCode,
                10, TimeUnit.MINUTES);


        // c12345,**minute**:5"

        System.out.println("开始调用openfein服务，进行调用第三方");
        feignService.sendCode(phone,code);

        System.out.println("调用结束");

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

    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult result, Model model){
        if(result.hasErrors()){
            Map<String, String> erros=new HashMap<>();
            erros=result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> fieldError.getField(),
                    fieldError -> fieldError.getDefaultMessage()));

            model.addAttribute("errors",erros);
            return "foward:/reg.html";
        }
        return "redirect:/login.html";
    }
}
