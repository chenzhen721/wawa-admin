package com.wawa.admin.web.crud

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import com.wawa.base.anno.RestWithSession
import com.wawa.base.persistent.KGS
import com.wawa.base.Crud
import com.wawa.base.BaseController
import com.wawa.api.Web
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.RandomUtils
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.groovy.CrudClosures.Int
import static com.wawa.groovy.CrudClosures.Str
import static com.wawa.groovy.CrudClosures.Timestamp

/**
 *
 */
@RestWithSession
class CashCodeController extends BaseController{

    DBCollection table(){
        adminMongo.getCollection('cash_code')
    }
    public static long DAY_MILLON = 24 * 3600 * 1000L;

    @Resource
    KGS    seqKGS
    @Delegate Crud crud = new Crud(table(),true,
            [_id:{seqKGS.nextId()},code:Str,status:Int,user_id:Int,timestamp:Timestamp,lastModif:Timestamp,
             stime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()},
             etime:{String str->  (str == null || str.isEmpty()) ? null : Web.getTime(str).getTime()}
            ],
             new Crud.QueryCondition(){
                public DBObject query(HttpServletRequest req) {
                    if (req['status']){
                        return QueryBuilder.start().and('status').is(req['status'] as Integer).get()
                    }
                    return super.query(req)
                }
                public DBObject sortby(HttpServletRequest req) {
                    return new BasicDBObject([status: 1, "stime":-1, "etime": 1])
                }
            }
    )

    //随机生成二维码
    def generate_code(HttpServletRequest req) {
        def s = ServletRequestUtils.getStringParameter(req, 'stime')
        def e = ServletRequestUtils.getStringParameter(req, 'etime')
        def n = ServletRequestUtils.getIntParameter(req, 'n')
        def stime = new Date().clearTime().getTime()
        def etime = new Date().clearTime().getTime() + DAY_MILLON
        if (StringUtils.isNotBlank(s)) {
            stime = Web.getTime(s).getTime()
        }
        if (StringUtils.isNotBlank(e)) {
            etime = Web.getTime(e).getTime()
        }
        int i = 0
        while (i < n) {
            def code = genCode(6)
            if (table().count(new BasicDBObject(code: code)) <= 0) {
                table().save(new BasicDBObject(_id: seqKGS.nextId(), code: code, status: 0,
                        stime: stime, etime: etime, timestamp:System.currentTimeMillis(),lastModif:System.currentTimeMillis()))
                i++
            }
        }
        return
    }

    private String s = "abcdefghijklmnopqrstuvwxyz1234567890"

    private String genCode(int len) {
        if (len <= 0) len = 6
        def r = ''
        for(int i = 0; i < len; i++) {
            r = r + s.charAt(RandomUtils.nextInt(s.length()))
        }
        return r
    }


}
