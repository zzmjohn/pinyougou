package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.entity.PageResult;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsExample;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojogroup.GoodsGroup;
import com.pinyougou.sellergoods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.alibaba.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static com.pinyougou.pojogroup.GoodsGroup.GOODS_UNAUDITED;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private TbGoodsMapper goodsMapper;

    @Autowired
    private TbGoodsDescMapper tbGoodsDescMapper;

    @Autowired
    private TbItemMapper tbItemMapper;

    @Autowired
    private TbItemCatMapper tbItemCatMapper;

    @Autowired
    private TbBrandMapper tbBrandMapper;

    @Autowired
    private TbSellerMapper tbSellerMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbGoods> findAll() {
        return goodsMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(GoodsGroup goods) {

        //设置商品状态
        goods.getTbGoods().setAuditStatus(GOODS_UNAUDITED);
        goodsMapper.insert(goods.getTbGoods());

        goods.getTbGoodsDesc().setGoodsId(goods.getTbGoods().getId());
        tbGoodsDescMapper.insert(goods.getTbGoodsDesc());

        if (isNotEmpty(goods.getItemCatList()) && "1".equals(goods.getTbGoods().getIsEnableSpec())) {
            goods.getItemCatList().stream().forEach(each -> {
                        //保存商品标题
                        StringBuffer sb = new StringBuffer(goods.getTbGoods().getGoodsName());
                        Map<String, Object> map = JSON.parseObject(each.getSpec());
                        map.forEach((k, v) -> {
                            sb.append(" " + v);
                        });
                        each.setTitle(sb.toString());

                        setTbItemValue(each, goods);

                        tbItemMapper.insert(each);
                    }
            );
        } else {//没有选中启用规格
            TbItem item = new TbItem();
            //名称
            item.setTitle(goods.getTbGoods().getGoodsName());
            //价格
            item.setPrice(goods.getTbGoods().getPrice());
            //数量
            item.setNum(9999);
            //状态
            item.setStatus("1");
            item.setIsDefault("1");
            item.setSpec(null);

            setTbItemValue(item, goods);

            tbItemMapper.insert(item);
        }
    }

    private void setTbItemValue(TbItem item, GoodsGroup goods) {
        //设置三级类别id
        item.setCategoryid(goods.getTbGoods().getCategory3Id());

        //设置创建时间和更新时间
        item.setCreateTime(new Date());
        item.setUpdateTime(new Date());

        //设置商品id
        item.setGoodsId(goods.getTbGoods().getId());
        //设置商家id
        item.setSellerId(goods.getTbGoods().getSellerId());

        //设置category分类名称
        item.setCategory(tbItemCatMapper.selectByPrimaryKey(goods.getTbGoods().getCategory3Id()).getName());

        //设置brand 品牌名称
        item.setBrand(tbBrandMapper.selectByPrimaryKey(goods.getTbGoods().getBrandId()).getName());

        //设置seller 商家名称
        item.setSeller(tbSellerMapper.selectByPrimaryKey(goods.getTbGoods().getSellerId()).getNickName());

        //设置商家图片
        List<Map> maps = JSON.parseArray(goods.getTbGoodsDesc().getItemImages(), Map.class);
        if (isNotEmpty(maps)) {
            String url = (String) maps.stream().findFirst().get().get("url");
            item.setImage(url);
        }
    }


    /**
     * 修改
     */
    @Override
    public void update(TbGoods goods) {
        goodsMapper.updateByPrimaryKey(goods);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbGoods findOne(Long id) {
        return goodsMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            goodsMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbGoodsExample example = new TbGoodsExample();
        Criteria criteria = example.createCriteria();

        if (goods != null) {
            if (goods.getSellerId() != null && goods.getSellerId().length() > 0) {
                criteria.andSellerIdEqualTo(goods.getSellerId());
            }
            if (goods.getGoodsName() != null && goods.getGoodsName().length() > 0) {
                criteria.andGoodsNameLike("%" + goods.getGoodsName() + "%");
            }
            if (goods.getAuditStatus() != null && goods.getAuditStatus().length() > 0) {
                criteria.andAuditStatusLike("%" + goods.getAuditStatus() + "%");
            }
            if (goods.getIsMarketable() != null && goods.getIsMarketable().length() > 0) {
                criteria.andIsMarketableLike("%" + goods.getIsMarketable() + "%");
            }
            if (goods.getCaption() != null && goods.getCaption().length() > 0) {
                criteria.andCaptionLike("%" + goods.getCaption() + "%");
            }
            if (goods.getSmallPic() != null && goods.getSmallPic().length() > 0) {
                criteria.andSmallPicLike("%" + goods.getSmallPic() + "%");
            }
            if (goods.getIsEnableSpec() != null && goods.getIsEnableSpec().length() > 0) {
                criteria.andIsEnableSpecLike("%" + goods.getIsEnableSpec() + "%");
            }
            if (goods.getIsDelete() != null && goods.getIsDelete().length() > 0) {
                criteria.andIsDeleteLike("%" + goods.getIsDelete() + "%");
            }

        }

        Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

}
