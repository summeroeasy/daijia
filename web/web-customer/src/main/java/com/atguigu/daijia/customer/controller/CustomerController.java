package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {

    @Autowired
    private CustomerService customerInfoService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerInfoService.login(code));
    }

    /**
     * 获取用户登陆信息
     * @return
     */
    @Operation(summary = "获取客户登录信息")
    @GuiguLogin
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo() {
        //1 从ThreadLocal当中获取用户id
        Long customerId = AuthContextHolder.getUserId();
        //调用service
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);
        return Result.ok(customerLoginVo);
    }

//    /**
//     * 获取用户登陆信息
//     * @param token 根据用户请求一起传递,在请求头中
//     * @return
//     */
//    @Operation(summary = "获取客户登录信息")
//    @GetMapping("/getCustomerLoginInfo")
//    public Result<CustomerLoginVo>
//    getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
//        //1 从请求头获取token字符串
////        HttpServletRequest request
////        String token = request.getHeader("token");
//        //调用service
//        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerLoginInfo(token);
//        return Result.ok(customerLoginVo);
//    }

    /**
     * 更新用户微信手机号
     * @param updateWxPhoneForm 用于存储customerId和wxPhone
     * @return
     */
    @Operation(summary = "更新用户微信手机号")
    @GuiguLogin
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        //个人版没有获取手机号权限
        //return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneForm));
        return Result.ok(true);
    }
}

