package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.groovy.CrudExt
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudExtClosures.IntNullable
import static com.wawa.common.doc.MongoKey.SJ_DESC
import static com.wawa.common.util.WebUtils.$$
import static com.wawa.groovy.CrudClosures.Bool
import static com.wawa.groovy.CrudClosures.Int
import static com.wawa.groovy.CrudClosures.Str
import static com.wawa.groovy.CrudClosures.Timestamp

/**
 * Created by Administrator on 2017/9/25.
 */
@RestWithSession
class ShopController extends BaseController{
    DBCollection table(){
        adminMongo.getCollection('shop')
    }
    DBCollection items() {
        adminMongo.getCollection('items')
    }

    @Resource
    KGS seqKGS

    //award_type: 0 优惠 award_type 1 首冲特权 不填无优惠, award: 统计用 充值时不产生影响
    private Map props = [_id             :{seqKGS.nextId()}, item_id:Str, name:Str, pic:Str, status:Bool, cost:Int, count:Int,
                 unit            :Str, limit:Int, desc:Str, tag:Str, timestamp:Timestamp, lastModif:Timestamp, group:Str,
                 after_award_desc:Str, after_award_diamond:IntNullable, award: Int,
                 after_award_days:IntNullable, award_type:IntNullable,
                 stime           :{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
                 order:{String str->  (str == null || str.isEmpty()) ? 1 : Integer.valueOf(str)  }
    ]

    private CrudExt crudExt = new CrudExt(table(), props, true)

    @Delegate Crud crud = new Crud(table(),true, props,
            new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if (req['status']){
                        return QueryBuilder.start().and('status').is(req['status'] as Boolean).get()
                    }
                    return super.query(req)
                }
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject([order: 1, status: 1, "stime":-1])
                }
            }
    )

    private static final Map vip = [_id: 'vip', name: 'vip', pic: ''] //TODO
    private static final Map coin = [_id: 'coin', name: '金币', pic: ''] //TODO

    def add(HttpServletRequest req) {
        return crudExt.add(req)
    }

    def edit(HttpServletRequest req) {
        return crudExt.edit(req)
    }

    /**
     * 可添加的商品列表
     * @param req
     * @return
     */
    def items(HttpServletRequest req) {
        Crud.list(req, items(), $$(status:1), $$(status:0), SJ_DESC) { List<BasicDBObject> list ->
            int page = Web.getPage(req)
            if (page == 1) {
                list.addAll([$$(vip), $$(coin)])
            }
        }
    }

}
