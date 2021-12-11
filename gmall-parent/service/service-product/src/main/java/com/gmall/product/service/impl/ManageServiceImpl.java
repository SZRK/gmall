package com.gmall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmall.common.constant.RedisConst;
import com.gmall.common.gmallannotation.GmallCache;
import com.gmall.model.product.*;
import com.gmall.product.mapper.*;
import com.gmall.product.service.ManageService;
import com.gmall.product.utils.FastDFSUtils;
import com.gmall.rabbitmq.constant.MqConst;
import com.gmall.rabbitmq.service.RabbitMQService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitMQService rabbitMQService;


    // 获取一级分类
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    // 根根据一级分类Id获取二级分类
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        return baseCategory2Mapper.selectList
                (new QueryWrapper<BaseCategory2>()
                        .eq("category1_id", category1Id));
    }

    // 根根据二级分类Id获取三级分类
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        return baseCategory3Mapper.selectList
                (new QueryWrapper<BaseCategory3>()
                        .eq("category2_id", category2Id));
    }

    // 根据分类id获取平台属性
    @Override
    public List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id) {

        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    // http://localhost/admin/product/attrInfoList/2/13/61

    // 添加平台属性
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 判断是添加还是修改
        if (baseAttrInfo.getId() != null) {
            // 如果是修改 则删除原来的attrValueList
            baseAttrInfoMapper.updateById(baseAttrInfo);
            // 修改平台属性值
            // 先获取平台属性值
            // 先删除旧的
            QueryWrapper queryWrapper = new QueryWrapper<BaseAttrValue>();
            queryWrapper.eq("attr_id", baseAttrInfo.getId());
            baseAttrValueMapper.delete(queryWrapper);
        }else {
            // 保存平台属性
            baseAttrInfoMapper.insert(baseAttrInfo); // 保存之后会将主键id封装到baseAt填入Info中
        }
            // 保存平台属性值
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            if (!CollectionUtils.isEmpty(attrValueList)) {
                attrValueList.forEach(attrValue -> {
                    attrValue.setAttrId(baseAttrInfo.getId());
                    baseAttrValueMapper.insert(attrValue);
                });
            }
    }
    // 根据平台属性ID获取平台属性
    @Override
    public List<BaseAttrValue> getAttrValueList(Long arrtId) {
        return baseAttrValueMapper.selectList(
                new QueryWrapper<BaseAttrValue>().eq("attr_id", arrtId));
    }

    @Override
    public IPage<BaseTrademark> selectPage(Integer page, Integer limit) {
        return baseTrademarkMapper.selectPage(
                new Page<BaseTrademark>(page, limit),null);
    }

    // 获取spu分页列表
    @Override
    public IPage<SpuInfo> getSpuInfoList(Integer page, Integer limit, Long category3Id) {
        return spuInfoMapper.selectPage(
                new Page<SpuInfo>(page, limit),
                new QueryWrapper<SpuInfo>().eq("category3_id", category3Id));
    }

    // 获取销售属性
    @Override
    public List<BaseSaleAttr> getbaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    // 获取品牌属性
    @Override
    public List<BaseTrademark> getTrademarkList() {
        return baseTrademarkMapper.selectList(null);
    }

    // 根据Id删除品牌
    @Override
    public void removeTtademarkById(Long traId) {
        // 查询图片Url
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(traId);
        // 删除品牌对应的Log_url在服务器中的图片
        if (!StringUtils.isEmpty(baseTrademark.getLogoUrl())) {
            FastDFSUtils.deleteFile(baseTrademark.getLogoUrl());
        }
        baseTrademarkMapper.deleteById(traId);
    }

    // 保存或修改Spu
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
      // 只有添加操作,进行添加操作
        spuInfoMapper.insert(spuInfo);
        // 添加对应的Image
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            spuImageList.forEach(spuImage -> {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }
        // 添加spu_sale_attr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                // 保存spu_sale_attr_value
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            });
        }

    }

    // 新增或删除品牌
    @Override
    public void saveTrademark(BaseTrademark baseTrademark) {

        if (baseTrademark.getId() != null) {
            // 修改,
            // 获取数据库中的图片地址
            BaseTrademark baseTrademark1 = baseTrademarkMapper.selectById(baseTrademark);
            if (baseTrademark1 != null &&
                    !baseTrademark1.getLogoUrl().equals(baseTrademark.getLogoUrl())) {
                FastDFSUtils.deleteFile(baseTrademark1.getLogoUrl());
            }
            // 修改数据
            baseTrademarkMapper.updateById(baseTrademark);
            return ;
        }

        // 新增
        baseTrademarkMapper.insert(baseTrademark);
    }

    // 根据品牌Id获取品牌
    @Override
    public BaseTrademark getTtademarkById(Long traId) {
        return baseTrademarkMapper.selectById(traId);
    }

    //获取sku分页列表
    @Override
    public IPage<SkuInfo> getSkuPage(Integer page, Integer limit) {
        return skuInfoMapper.selectPage(new Page<SkuInfo>(page, limit), null);
    }

    // 根据spuId获取图片列表
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id", spuId));
    }

    // 根据spuId获取销售属性
    @Override
    public List<SpuSaleAttr> getspuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.getspuSaleAttrList(spuId);
    }

    // 添加sku
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        // 添加sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.forEach(skuImage -> {
                // 填充skuId属性
                skuImage.setSkuId(skuInfo.getId());
                // 添加到sku_image
                skuImageMapper.insert(skuImage);
            });
        }
        // sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }
        //sku_sale_attr_value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }
    }

    // 上架
    @Override
    public void onSale(Long skuId) {
        // 先查询
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        skuInfo.setIsSale(SkuInfo.ONSALE);
        // 修改
        rabbitMQService.sentMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);
        skuInfoMapper.updateById(skuInfo);
    }
    // 下架
    @Override
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        skuInfo.setIsSale(SkuInfo.CANCELSALE);
        // 修改
        rabbitMQService.sentMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);

        skuInfoMapper.updateById(skuInfo);
    }

    // 根据skuId获取sku信息
    // 进阶添加redis缓存
    /**
     * 1.先查询redis缓存，redis缓存有直接返回。
     * 2.redis缓存没有查询数据库。
    *  --|2.1：数据库有将查询到的数据放入到redis缓存中并设置随机过期时间，防止缓存雪崩
     * --|2.2：数据库没有则保存null值到redis缓存中，防止小范围缓存穿透，（布隆过滤器）
     * 3.解决缓存击穿：
     * --|3.1：加分布式锁，
     * 注意事项
     * --|1：互斥性（redis的setnx命令）
     * --|2：可重入性 （redisson底层完成）
     * --|3：防死锁：
     *  --|3.1：在finally块中进行解锁
     *  --|3.2：设置锁的过期时间
     * --|不能解除其他人的锁
     * --|过期时间内续约
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        // 查询sku信息
        //1: 查询redis缓存，
        String cacheKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX + "YC"; // 获取key
        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);
        if (null == skuInfo) {
            // redis中没有数据 去数据库中查询数据, *此时可能出现缓存击穿问题，需要使用分布式锁
            String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX + "YC";
            String lockValue = UUID.randomUUID().toString() + Thread.currentThread().getName();
            // 尝试获取锁， 防止死锁需要加过期时间。还需要添加唯一Id，防止解锁的时候解除了别人的锁
            // 改进 使用redisson
            RLock lock = redissonClient.getLock(lockKey);
          /*
            Boolean isLock = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,
              RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);*/
            try {
                if (lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS)) {
                    try {
                        // 获取到锁
                        skuInfo = skuInfoMapper.selectById(skuId);
                        if (null != skuInfo) {
                            // 查询图片信息
                            List<SkuImage> list = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
                            skuInfo.setSkuImageList(list);
                            // 将数据保存到redis缓存中,为了防止缓存雪崩，还需要差异过期时间
                            redisTemplate.opsForValue().set(cacheKey, skuInfo,
                                    RedisConst.SKUKEY_TIMEOUT + new Random().nextInt(300), TimeUnit.SECONDS);
                        } else {
                            // 数据库中也没有数据, 保存null值，防止缓存穿透
                            redisTemplate.opsForValue().set(cacheKey, new SkuInfo(),
                                    RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        }

                    } finally {
                        // 解锁操作， 解锁操作必须保证原子性及不能解除别人的锁,使用哪个lua脚本
                      /*  String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return tostring(redis.call('del',KEYS[1])) else return 0 end";
                        //执行此脚本    Redis是单线程 （Redis自己就是原子性
                        this.redisTemplate.execute(new DefaultRedisScript<>(script), Collections.singletonList(lockKey), lockValue);*/
                        lock.unlock();
                    }
                } else {
                    // 没有获取到锁，阻塞一会儿再来获取缓存中的数据
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    skuInfo = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return skuInfo;
    }

    // 通过三级分类id查询分类信息
    @Override
    @GmallCache(prefix = "getCategoryView")
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectOne(new QueryWrapper<BaseCategoryView>().eq("category3_id", category3Id));
    }

    //获取sku最新价格
    @Override

    public BigDecimal getSkuPrice(Long skuId) {
        //方式一：
       /* SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null != skuInfo) {
            return skuInfo.getPrice();
        }*/
       // 方式二：只查询price字段
        QueryWrapper<SkuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", skuId);
        queryWrapper.select("price");
        SkuInfo skuInfo = skuInfoMapper.selectOne(queryWrapper);

        if (null != skuInfo) {
            return skuInfo.getPrice();
        }
        return null;
    }

    // 根据spuId，skuId 查询销售属性集合
    @Override
    @GmallCache(prefix = "selectSpuSaleAttrListCheckBySku")
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        List<SpuSaleAttr> list = spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId, spuId);
        return list;
    }
    // 根据spuId 查询map 集合属性
    @Override
    @GmallCache(prefix = "getSkuValueIdsMap")
    public Map getSkuValueIdsMap(Long spuId) {

        List<Map> skuValueIdsMap = skuSaleAttrValueMapper.getSkuValueIdsMap(spuId);
        HashMap<Object, Object> result = new HashMap<>();
        if  (!CollectionUtils.isEmpty(skuValueIdsMap)) {
            skuValueIdsMap.forEach(map -> {
                result.put(map.get("value_ids"), map.get("sku_id"));
            });
        }
        return result;
    }

    // 查询分类视图中所有的分类数据
    @Override
    public List<BaseCategoryView> getBaseCategoryViewList() {
        return baseCategoryViewMapper.selectList(null);
    }

    //查询品牌数据
    @Override
    public BaseTrademark getTrademark(long tmId) {
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(tmId);
        return baseTrademark;
    }

    // 查询库存属性对应的平台属性及属性值
    @Override
    public List<SkuAttrValue> getAttrAndAttrValueByskuId(Long skuId) {
        return skuAttrValueMapper.getAttrAndAttrValueByskuId(skuId);
    }

}
