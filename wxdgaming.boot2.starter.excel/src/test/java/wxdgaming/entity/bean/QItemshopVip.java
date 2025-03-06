package wxdgaming.entity.bean;


import lombok.Getter;
import wxdgaming.boot2.starter.excel.store.DataChecked;
import wxdgaming.entity.bean.mapping.QItemshopVipMapping;

import java.io.Serializable;


/**
 * excel 构建 vip礼包, src/main/resources/范例.xlsx, q_itemshop_vip,
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-06 21:12:35
 **/
@Getter
public class QItemshopVip extends QItemshopVipMapping implements Serializable, DataChecked {

    @Override public void initAndCheck() throws Exception {
        /*todo 实现数据检测和初始化*/

    }

}
