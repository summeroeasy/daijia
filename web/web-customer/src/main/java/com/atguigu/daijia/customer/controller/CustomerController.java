package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Autowired
    private OrderService orderService;

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

    // 获取乘客订单分页列表的接口
    // 使用了GuiguLogin注解，表示需要用户登录后才能访问此接口
    // 接口的请求方式为GET，路径为/findCustomerOrderPage/{page}/{limit}
    @GetMapping("findCustomerOrderPage/{page}/{limit}")
    public Result<PageVo> findCustomerOrderPage(
            // 当前页码，作为路径变量传入
            @PathVariable Long page,
            // 每页记录数，作为路径变量传入
            @PathVariable Long limit) {
        // 从认证令牌中获取当前用户ID
        Long customerId = AuthContextHolder.getUserId();
        // 调用订单服务的findCustomerOrderPage方法，传入用户ID、当前页码和每页记录数，获取分页订单信息
        PageVo pageVo = orderService.findCustomerOrderPage(customerId, page, limit);
        // 返回成功结果，携带分页订单信息
        return Result.ok(pageVo);
    }
}

