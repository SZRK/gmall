package com.gmall.cart.service.impl;

import com.gmall.cart.mapper.CartInfoMapper;
import com.gmall.cart.service.CartAsyncService;
import com.gmall.cart.service.CartService;
import com.gmall.common.constant.RedisConst;
import com.gmall.model.cart.CartInfo;
import com.gmall.model.product.SkuInfo;
import com.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
}
