package com.atguigu.daijia.payment.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.payment.config.WxPayV3Properties;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import com.atguigu.daijia.payment.service.WxPayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;

    @Autowired
    private WxPayV3Properties wxPayV3Properties;

    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {
        try {
            //1 添加支付记录到支付表里面
            //判断： 如果表存在订单支付记录，不需要添加
            LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo());
            PaymentInfo paymentInfo = paymentInfoMapper.selectOne(queryWrapper);
            if (paymentInfo == null){
                paymentInfo = new PaymentInfo();
                BeanUtils.copyProperties(paymentInfoForm, paymentInfo);
                paymentInfo.setPaymentStatus(0);
                paymentInfoMapper.insert(paymentInfo);
            }

            //2 创建微信支付使用对象
            JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                    .config(rsaAutoCertificateConfig)
                    .build();

            //3 创建request对象，封装微信对象使用参数
            PrepayRequest request =  new PrepayRequest();
            Amount amount = new Amount();
            amount.setTotal(paymentInfoForm.getAmount().multiply(new BigDecimal(100)).intValue());
            request.setAmount(amount);
            request.setAppid(wxPayV3Properties.getAppid());
            request.setMchid(wxPayV3Properties.getMerchantId());
            //string[1,127]
            String description = paymentInfo.getContent();
            if(description.length() > 127) {
                description = description.substring(0, 127);
            }
            request.setDescription(description);
            request.setNotifyUrl(wxPayV3Properties.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());
            //获取用户信息
            Payer payer = new Payer();
            payer.setOpenid(paymentInfoForm.getCustomerOpenId());
            request.setPayer(payer);

            //是否指定分账，不指定不能分账
            SettleInfo settleInfo = new SettleInfo();
            settleInfo.setProfitSharing(true);
            request.setSettleInfo(settleInfo);
            //4 调用微信支付使用对象里面的方法实现微信对象调用
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            //5 根据返回对象，封装到WxPrepayVo对象中
            WxPrepayVo wxPrepayVo = new WxPrepayVo();
            BeanUtils.copyProperties(response, wxPrepayVo);
            wxPrepayVo.setTimeStamp(response.getTimeStamp());
            return wxPrepayVo;
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }
}
