package com.wawa.admin.web
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.QueryBuilder
import com.wawa.base.BaseController
import com.wawa.base.Crud
import com.wawa.base.anno.RestWithSession
import com.wawa.common.util.JSONUtil

import com.wawa.api.Web
import com.wawa.common.util.KeyUtils
import com.wawa.common.util.Regex
import com.wawa.model.OpType

import javax.servlet.http.HttpServletRequest

import static com.wawa.common.doc.MongoKey.*
import static com.wawa.common.util.WebUtils.$$

import org.apache.commons.lang.StringUtils
import static com.wawa.groovy.CrudClosures.Int

/**
 * date: 13-3-28 下午2:31
 * @author: yangyang.cong@ttpod.com
 */
//@Rest
@RestWithSession
class SysController extends BaseController{

    DBCollection table(){adminMongo.getCollection('config')}

    DBCollection informs(){adminMongo.getCollection('informs')}

    def pub_notice(HttpServletRequest request){
        publish(KeyUtils.CHANNEL.all(),[
                action:'sys.notice',
                data_d:[
                        msg:request.getParameter('msg'),
                        url:request.getParameter('url')
                ] as Map<String, Object>
        ])
        OK()
    }

    static final String download = 'download'
    static final String[] download_fields=["ver","sj","img_url","down_url","name","size","des","pic_des","mobile_down_url"]

    def save_download_info(HttpServletRequest req){

        String id = req.getParameter(_id)
        if (StringUtils.isBlank(id))
            id =  download

        def info  = new BasicDBObject(_id,id)
        for(String field : download_fields){
            info.put(field,req.getParameter(field))
        }
        info.put(timestamp,System.currentTimeMillis())
        if(table().save(info).getN() == 1){
            Crud.opLog("sys_save_download_info",info)
            /*StaticSpring.execute(new Runnable() {
                @Override
                void run() {
                    Thread.sleep(10000L) // sleep a while to wait mongodb data resync
                    for (String file  : ['download']){//,'mobile']){
                        def prc = ['/empty/crontab/static.sh',"${file}.xhtml","/empty/show.dongting.com/${file}.html"]
                        prc.execute().waitForProcessOutput(NULL_OUTPUT_STREAM, NULL_OUTPUT_STREAM)
                    }
                }
            })*/
        }
        OK()
    }

    def show_download_info(){
         def query = $$(_id, [$in: ['download','download1','download2','download3']])
       //def query = $$(_id, [$in: ['download']])
        def result = table().find(query,ALL_FIELD).toArray()
        [code:1,data:result]
    }

    static final String special_gifts = 'special_gifts'
    static final Map<String, Closure> special_gifts_fields=[stime:Int,etime:Int,ratio:{it as Double},
            seeds:{String str->str.split('_').collect {Integer.valueOf(it.toString())}  }]

    def save_special_gifts(HttpServletRequest req){
        def info  = new BasicDBObject(_id,special_gifts)
        special_gifts_fields.each{String field,Closure v->
            info.put(field,v.call(req.getParameter(field)))
        }
        def seeds  = (List)info.get("seeds")
        info.put("gift",seeds.get(new Random().nextInt(seeds.size())))
//        info.put(timestamp,System.currentTimeMillis())
        if(table().save(info).getN() == 1){
            Crud.opLog("sys_save_special_gifts",info)
        }
        OK()
    }

    def show_special_gifts(){
        def config = table().findOne(special_gifts)
        if(null != config){
            config.put("seeds",adminMongo.getCollection('gifts').find($$(_id,$$($in,config.get("seeds")))).toArray())
        }
        [code:1,data:config]
    }


    def oplog_list(HttpServletRequest req){
        def qb = Web.fillTimeBetween(req)
        stringQuery(qb,req,'type')
        String uid = req.getParameter('_id');
        if (StringUtils.isNotBlank(uid)) {
            qb.and('session._id').is(uid);
        }
        Crud.opLoglist(req,qb.get())
    }

    def fill_bag(HttpServletRequest req){
        def query = new BasicDBObject()
        for(String field  : ['priv',_id]){
            String val = req.getParameter(field)
            if(Regex.isVaildIds(val)){
                query.put(field,$$($in ,val.split('_').collect {it as Integer}))
            }
        }

        if(query.isEmpty()){
            return [code:0,msg: "priv or _id must not ALL NULL"]
        }

        def gifts = JSONUtil.jsonToMap(req.getParameter("gifts"))
        def inc = new BasicDBObject()
        for(Map.Entry entry :gifts.entrySet()){
            inc.put("bag.${entry.getKey()}".toString(),((Number)entry.getValue()).intValue())
        }
        if(inc.isEmpty()){
            return [code:0,msg: "gifts must not NULL"]
        }

        def rows = users().updateMulti(query,new BasicDBObject($inc,inc)).getN()
        Crud.opLog(OpType.sys_fill_bag,[query:[_id:req.getParameter(_id),priv:req.getParameter('priv')],gifts:gifts,rows:rows])
        OK()
    }


    def list_inform(HttpServletRequest req){
        QueryBuilder query = QueryBuilder.start();

        String id = req.getParameter(_id)
        if (StringUtils.isNotBlank(id))
            query.and("_id").is(id)

        Crud.list(req,informs(),query.get(),ALL_FIELD,SJ_DESC)
    }

    /**
     * type: 1:PC   2:手机客户端
     * @param req
     * @return
     */
    def add_inform(HttpServletRequest req)
    {
        String msg = req.getParameter('msg') as String
        String url = req.getParameter('url') as String
        Integer type = req.getParameter('type') as Integer
        long begin = Web.getStime(req).getTime()
        long end = Web.getEtime(req).getTime()
        Long tmp = System.currentTimeMillis()
        def prop = [
                _id:tmp,
                msg:msg,
                url:url,
                stime:begin,
                etime:end,
                type:type,
                timestamp: tmp
        ]
        informs().save(new BasicDBObject((Map)prop))
        Crud.opLog(OpType.informs_add,prop)
        [code:1]
    }

    def edit_inform(HttpServletRequest req)
    {
        String msg = req.getParameter('msg') as String
        String url = req.getParameter('url') as String
        Long _id = req.getParameter('_id') as Long
        Integer type = req.getParameter('type') as Integer
        long begin = Web.getStime(req).getTime()
        long end = Web.getEtime(req).getTime()
        def prop = [
                msg:msg,
                url:url,
                type:type,
                stime:begin,
                etime:end
        ]
        informs().update(new BasicDBObject(_id:_id),new BasicDBObject($set,new BasicDBObject((Map)prop)))
        Crud.opLog(OpType.informs_add,prop)
        [code:1]
    }

    def del_inform(HttpServletRequest req){
        Long _id = req.getParameter('_id') as Long
        informs().remove($$(_id:_id))
        Crud.opLog(OpType.informs_del,[_id:_id])
        [code:1]
    }
}
