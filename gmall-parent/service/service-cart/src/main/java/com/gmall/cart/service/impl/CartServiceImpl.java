package com.gmall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.cart.service.CartAsyncService;
import com.gmall.cart.service.CartService;
import com.gmall.common.constant.RedisConst;
import com.gmall.model.cart.CartInfo;
import com.gmall.model.order.OrderDetail;
import com.gmall.model.product.SkuInfo;
import com.gmall.model.user.UserAddress;
import com.gmall.product.client.ProductFeignClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CartAsyncService cartAsyncService;

    @Override
    public CartInfo addToCart(Long skuId, Integer skuNum, String userId) {
        // 保存到redis，并将数据保存到数据库中
        // 构建redis中的缓存key
        String cartCacheKey = RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
        // 先从redis中获取当前key，
        CartInfo cartInfo
                = (CartInfo) redisTemplate.opsForHash().get(cartCacheKey, skuId.toString());
        if (null != cartInfo) {
            // 做修改操作
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
            cartInfo.setIsChecked(CartInfo.ISCHECK);
            // 异步更新数据库
            cartAsyncService.updateCart(cartInfo);
        } else {
            // 做添加操作
            cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setIsChecked(CartInfo.ISCHECK);

            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            //放入购物车时的价格
            BigDecimal price = productFeignClient.getSkuPrice(skuId);
            cartInfo.setCartPrice(price);
            //真正实时价格
            cartInfo.setSkuPrice(price);
            // 异步新增数据库
            cartAsyncService.insertCart(cartInfo);
        }
        //追加数据或 添加新商品
        redisTemplate.opsForHash().put(cartCacheKey,skuId.toString(),cartInfo);
        //返回当前购物车
        return cartInfo;
    }

    @Override
    public CartInfo getCartInfo(String userId, Long skuId) {
        String cartCacheKey = RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
        return (CartInfo) redisTemplate.opsForHash().get(cartCacheKey, skuId.toString());
    }

    // 查询并合并未登录的购物车信息，已登录的购物车信息
    @Override
    public List<CartInfo> getcartList(String userId, String userTempId) {
        if (!StringUtils.isEmpty(userId)) {
            if (!StringUtils.isEmpty(userTempId)) {
                // 真实用户不为空, 临时用户不为空
                return mergeCartInfoList(userId, userTempId);
            }
            // 真实用户部位空，临时用户为空
            return getCartInfoList(userId);
        }
        // 只有临时Id中的购物车数据,
        // 查询临时Id中购物车中的集合并返回
        if (!StringUtils.isEmpty(userTempId)) {
          return   getCartInfoList(userTempId);
        }
        return null;
    }

    @Override
    public void checkCart(Long skuId, Integer isChecked, String userId) {
        // 获取当前商品
        String cartCacheKeyByUserId =  RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().
                get(cartCacheKeyByUserId, skuId.toString());
        cartInfo.setIsChecked(isChecked);
        // 更新redis
        redisTemplate.opsForHash().put(cartCacheKeyByUserId, skuId.toString(), cartInfo);
        // 更新数据库
        cartAsyncService.updateCart(cartInfo);
    }

    // 删除购物车中的购物项
    @Override
    public void deleteCart(Long skuId, String userId) {
        String cartCacheKeyByUserId =  RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
        // 输出redis中的数据
        redisTemplate.opsForHash().delete(cartCacheKeyByUserId, skuId.toString());
        //  删除数据库中的数据
        cartAsyncService.deleteCartInfoByUserTempIdAndSkuId(userId, skuId);
    }
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        String cartCacheKey = RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
      return redisTemplate.opsForHash().values(cartCacheKey);
    }


    // 合并临时购物车和当前用户购物车中的集合
    private List<CartInfo> mergeCartInfoList(String userId, String userTempId) {
        List<CartInfo> cartInfoListByUserId = getCartInfoList(userId);
        List<CartInfo> cartInfoListByUserTempId = getCartInfoList(userTempId);
        String cartCacheKeyByUserId =  RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
        String cartCacheKeyByUserTempId = RedisConst.USER_KEY_PREFIX + userTempId
                + RedisConst.USER_CART_KEY_SUFFIX;
        if (!CollectionUtils.isEmpty(cartInfoListByUserId)) {
            if (!CollectionUtils.isEmpty(cartInfoListByUserTempId)) {
                    // 两个购物车中的集合都不为空
                    Map<Long, CartInfo> cartInfoMapByUserId = cartInfoListByUserId.stream()
                            .collect(Collectors.toMap(
                                    CartInfo::getSkuId,
                                    cartInfo -> cartInfo
                            ));
                // 遍历cartInfoListByUserTempId，判断当前cartInfo是否在cartInfoMapByUserId中
                for (CartInfo cartInfo : cartInfoListByUserTempId) {
                    if (cartInfoMapByUserId.containsKey(cartInfo.getSkuId())) {
                        // 含有Key则更新数量
                        cartInfo.setSkuNum(cartInfo.getSkuNum() +
                                cartInfoMapByUserId.get(cartInfo.getSkuId()).getSkuNum());
                        cartInfo.setUserId(userId);
                        cartInfo.setIsChecked(CartInfo.ISCHECK);
                        cartInfoMapByUserId.put(cartInfo.getSkuId(), cartInfo);
                        // 更新redis
                        redisTemplate.opsForHash().put(cartCacheKeyByUserId, cartInfo.getSkuId().toString(), cartInfo);
                        // 更新数据库
                        cartAsyncService.updateCart(cartInfo);
                        // 删除临时用户购物车中的数据
                        cartAsyncService.deleteCartInfoByUserTempIdAndSkuId(userTempId, cartInfo.getSkuId());
                    } else {
                        // 不含有当前Key 则在数据库及redis中添加当前数据，
                        // 并修改临时UserId为真正登录的用户Id，并将当前数据添加到cartInfoMapByUserId中
                        cartInfo.setUserId(userId);
                        cartInfo.setIsChecked(CartInfo.ISCHECK);
                        cartInfoMapByUserId.put(cartInfo.getSkuId(), cartInfo);
                        // 修改reids中的购物车信息中的user_id
                        redisTemplate.opsForHash().put(cartCacheKeyByUserId, cartInfo.getSkuId().toString(), cartInfo);
                        // 添加到数据库
                        cartAsyncService.insertCart(cartInfo);
                        //删除数据库中的临时用户购物车中的此商品
                        cartAsyncService.deleteCartInfoByUserTempIdAndSkuId(userTempId, cartInfo.getSkuId());
                    }
                }
                // 统一删除临时用户购物车中的数据
                redisTemplate.delete(cartCacheKeyByUserTempId);
                return new ArrayList<CartInfo>(cartInfoMapByUserId.values());
            }
            return cartInfoListByUserId;
        }
        // cartInfoListByUserId为空， 则将cartInfoListByUserTempId中的内容保存到redis和数据库中
        if (!CollectionUtils.isEmpty(cartInfoListByUserTempId)) {
            // cartInfoListByUserId为空， 则将cartInfoListByUserTempId中的内容保存到redis和数据库中
            Map<String, CartInfo> cartInfoMapByUserTempId = cartInfoListByUserTempId.stream().collect(Collectors.toMap(
                    cartInfo -> {
                        return cartInfo.getSkuId().toString();
                    },
                    cartInfo -> {
                        cartInfo.setIsChecked(CartInfo.ISCHECK);
                        cartInfo.setUserId(userId);
                        return cartInfo;
                    }
            ));
            // 添加到redis
            redisTemplate.opsForHash().putAll(cartCacheKeyByUserId, cartInfoMapByUserTempId);
            // 删除临时用户的购物出
            redisTemplate.delete(cartCacheKeyByUserTempId);
            // 修改DB中的Id为UserId
            cartAsyncService.updateCartUserId(userId, userTempId);
            return cartInfoListByUserTempId;
        }
        return null;
    }

    // 获取购物车中的集合
    private List<CartInfo> getCartInfoList(String userId) {
        String cartCacheKey = RedisConst.USER_KEY_PREFIX + userId
                + RedisConst.USER_CART_KEY_SUFFIX;
        List<CartInfo> list = redisTemplate.opsForHash().values(cartCacheKey);
        // 购物车中的价格为非实时价格，需要查询出实时价格后修改为实时价格
        list.stream().forEach(cartInfo -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
        });
        return list;
    }

}
