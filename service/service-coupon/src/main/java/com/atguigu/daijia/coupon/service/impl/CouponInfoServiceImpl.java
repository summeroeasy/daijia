package com.atguigu.daijia.coupon.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.coupon.mapper.CouponInfoMapper;
import com.atguigu.daijia.coupon.mapper.CustomerCouponMapper;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.entity.coupon.CustomerCoupon;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.AvailableCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    @Autowired
    private CouponInfoMapper couponInfoMapper;

    @Autowired
    private CustomerCouponMapper customerCouponMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoReceiveCouponVo> pageInfo = couponInfoMapper.findNoReceivePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
        Page<UsedCouponVo> pageInfo = couponInfoMapper.findUsedPage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = couponInfoMapper.findNoUsePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    /**
     * 用户领取优惠券
     *
     * @param customerId 用户ID
     * @param couponId   优惠券ID
     * @return 返回true表示领取成功，false表示领取失败
     */
    @Override
    public Boolean receive(Long customerId, Long couponId) {
        //1 查询优惠券
        CouponInfo couponInfo = this.getById(couponId);
        if (null == couponInfo) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //2 优惠券过期时间判断
        if (couponInfo.getExpireTime().before(new Date())) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
        }

        //3 校验库存，优惠券领取数量判断
        if (couponInfo.getPublishCount() != 0 && couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }

        RLock lock = null;
        try {
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
            boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME, RedisConstant.COUPON_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (flag) {
                //4 校验每人限领数量
                if (couponInfo.getPerLimit() > 0) {
                    //4.1、统计当前用户对当前优惠券的已经领取的数量
                    long count = customerCouponMapper
                            .selectCount(new LambdaQueryWrapper<CustomerCoupon>()
                                    .eq(CustomerCoupon::getCouponId, couponId)
                                    .eq(CustomerCoupon::getCustomerId, customerId));

                    //4.2、校验限领数量
                    if (count >= couponInfo.getPerLimit()) {
                        throw new GuiguException(ResultCodeEnum.COUPON_USER_LIMIT);
                    }

                }
                //5 更新优惠券数量
                int row = couponInfoMapper.updateReceiveCount(couponId);
                if (row == 1) {
                    //6、保存领取记录
                    this.saveCustomerCoupon(customerId, couponId, couponInfo.getExpireTime());
                    return true;
                }
            }
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        return true;
    }

    /**
     * 根据乘客ID和订单金额查找可用的优惠券
     *
     * @param customerId  乘客ID
     * @param orderAmount 订单金额
     * @return 返回一个包含可用优惠券的列表
     */
    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {
        //1 创建list集合，存储最终返回数据
        List<AvailableCouponVo> availableCouponVoList = new ArrayList<>();

        //2 根据乘客id，获取乘客已经领取但是没有领取的优惠券列表
        List<NoUseCouponVo> list = couponInfoMapper.findNoUseList(customerId);

        //3 遍历优惠券列表，判断优惠券是否满足使用条件
        //3.1 判断优惠卷类型：现金卷 和 折扣卷
        List<NoUseCouponVo> typeList = list.stream().filter(item -> item.getCouponType() == 1).collect(Collectors.toList());

        //3.2 是现金券
        //判断现金券是否满足条件
        for (NoUseCouponVo noUseCouponVo : typeList) {
            //判断使用金额
            //减免金额
            BigDecimal reduceAmount = noUseCouponVo.getAmount();

            //1 没有门槛  == 0，订单金额必须大于优惠减免金额
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0
                    && orderAmount.subtract(reduceAmount).doubleValue() >= 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }

            //2 有门槛，订单金额必须大于等于优惠减免金额
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0
                    && orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }

        //3.3 是折扣卷
        // 判断折扣券是否满足条件
        List<NoUseCouponVo> typeList2 = list.stream().filter(item -> item.getCouponType() == 2).collect(Collectors.toList());
        for (NoUseCouponVo noUseCouponVo : typeList2) {
            //折扣之后金额
            BigDecimal discountAmount = orderAmount
                    .multiply(noUseCouponVo.getDiscount())
                    .divide(BigDecimal.valueOf(10))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal reduceAmount = orderAmount.subtract(discountAmount);
            //2.2.1.没门槛
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0
                    && discountAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }


        //4 把满足条件的优惠券放到最终list集合
        //根据金额进行排序
        if (!CollectionUtils.isEmpty(availableCouponVoList)) {
            Collections.sort(availableCouponVoList, new Comparator<AvailableCouponVo>() {
                @Override
                public int compare(AvailableCouponVo o1, AvailableCouponVo o2) {
                    return o1.getAmount().compareTo(o2.getAmount());
                }
            });
        }
        return availableCouponVoList;
    }

    @Transactional(noRollbackFor = Exception.class)
    @Override
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        //1 获取乘客优惠券
        CustomerCoupon customerCoupon =
                customerCouponMapper.selectById(useCouponForm.getCustomerCouponId());
        if (null == customerCoupon) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }

        //2 获取优惠券信息
        CouponInfo couponInfo =
                couponInfoMapper.selectById(customerCoupon.getCouponId());
        if (null == couponInfo) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }

        //3 判断优惠券是否为该用户所有
        if (customerCoupon.getCustomerId().longValue() != useCouponForm.getCustomerId().longValue()) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //获取优惠券减免金额
        BigDecimal reduceAmount = null;
        if (couponInfo.getCouponType() == 1) {
            //使用门槛判断
            //2.1.1.没门槛，订单金额必须大于优惠券减免金额
            if (couponInfo.getConditionAmount().doubleValue() == 0
                    && useCouponForm.getOrderAmount().subtract(couponInfo.getAmount()).doubleValue() > 0) {
                //减免金额
                reduceAmount = couponInfo.getAmount();
            }
            //2.1.2.有门槛，订单金额大于优惠券门槛金额
            if (couponInfo.getConditionAmount().doubleValue() > 0
                    && useCouponForm.getOrderAmount().subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
                //减免金额
                reduceAmount = couponInfo.getAmount();
            }
        } else {
            //使用门槛判断
            //订单折扣后金额
            BigDecimal discountOrderAmount = useCouponForm
                    .getOrderAmount()
                    .multiply(couponInfo.getDiscount())
                    .divide(new BigDecimal("10"))
                    .setScale(2, RoundingMode.HALF_UP);
            //订单优惠金额
            //2.2.1.没门槛
            if (couponInfo.getConditionAmount().doubleValue() == 0) {
                //减免金额
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
            //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
            if (couponInfo.getConditionAmount().doubleValue() > 0
                    && discountOrderAmount.subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
                //减免金额
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
        }
        //5 满足条件，更新数据
        if (reduceAmount.doubleValue() > 0) {
            //更新coupon_info使用数量
            //根据id查询优惠卷对象
            Integer useCount_old = couponInfo.getUseCount();
            couponInfo.setUseCount(useCount_old + 1);
            couponInfoMapper.updateById(couponInfo);

            //更新customer_coupon
            CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
            updateCustomerCoupon.setId(customerCoupon.getId());
            updateCustomerCoupon.setUsedTime(new Date());
            updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
            customerCouponMapper.updateById(updateCustomerCoupon);
        }
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    private void saveCustomerCoupon(Long customerId, Long couponId, Date expireTime) {
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus(1);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setExpireTime(expireTime);
        customerCouponMapper.insert(customerCoupon);
    }

    private AvailableCouponVo buildBestNoUseCouponVo(NoUseCouponVo noUseCouponVo, BigDecimal reduceAmount) {
        AvailableCouponVo bestNoUseCouponVo = new AvailableCouponVo();
        BeanUtils.copyProperties(noUseCouponVo, bestNoUseCouponVo);
        bestNoUseCouponVo.setCouponId(noUseCouponVo.getId());
        bestNoUseCouponVo.setReduceAmount(reduceAmount);
        return bestNoUseCouponVo;
    }
}
