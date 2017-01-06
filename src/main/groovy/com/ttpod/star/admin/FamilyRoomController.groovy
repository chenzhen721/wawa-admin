package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.common.doc.ParamKey
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.model.ApplyType
import com.ttpod.star.model.OpType
import com.ttpod.star.model.RoomType
import com.ttpod.star.model.UserType
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.SJ_DESC
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * 家族直播间
 */
//@Rest
@RestWithSession
class FamilyRoomController extends BaseController{

    DBCollection table(){adminMongo.getCollection('froom_applys')}

    /**
     * 家族直播间列表
     * @param req
     * @return
     */
    def list(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        ['_id','family_id'].each {String field->
            intQuery(query,req,field)
        }
        query.and('type').is(Integer.valueOf(RoomType.家族.ordinal()));
        Crud.list(req,rooms(),query.get(),new BasicDBObject('sfz_pic',0),SJ_DESC)
    }

    /**
     * 关闭家族房间
     * @param req
     * @return
     */
    def terminate(HttpServletRequest req) {
        def roomId = req.getInt(_id)
        def room = rooms().findOne($$(_id, roomId))
        if(room){
            Integer family_id = room.get('family_id') as Integer
            rooms().remove($$(_id : roomId, family_id:family_id))
            def apply = table().findOne($$('family_id': family_id, status: ApplyType.通过.ordinal()), new BasicDBObject('status', 1), SJ_DESC)
            if (apply) {
                def tmp = System.currentTimeMillis()
                table().update(apply, new BasicDBObject('$set',[status: ApplyType.解约.ordinal(), lastModif: tmp]))
            }
            Crud.opLog(OpType.family_room_terminate, [family_id: family_id, roomId: roomId])
        }
        return OK()
    }

    /**
     * 开通家族房间
     * @param req
     * @return
     */
    def open(HttpServletRequest req){
        def family_id = req.getInt('family_id')
        if(rooms().count($$(family_id:family_id)) == 1){
            return [code : 1]
        }
        def family = familyMongo.getCollection('familys').findOne($$(_id:family_id,"status": ApplyType.通过.ordinal()))
        if (null == family)
            return Web.notAllowed()

        def name = family.get('family_name') as String
        Long time = System.currentTimeMillis()
        rooms().update($$(_id,family_id),$$($set:[xy_star_id:null,live:Boolean.FALSE, bean : 0, visiter_count:0,found_time:time,pic_url:"",greetings:"",
                                                  room_ids:family_id.toString(),name: name,family_id:family_id,type: RoomType.家族.ordinal()]),true, false, writeConcern)

        Crud.opLog(OpType.family_room_open,[family_id:family_id])
        OK()
    }

    // 签约申请查询
    def apply_list(HttpServletRequest req){
        def query = Web.fillTimeBetween(req)
        query = Web.fillTimeBetween(query, "lastmodif", req)
        ['status','user_id'].each {String field->
            intQuery(query,req,field)
        }
        Crud.list(req,table(),query.get(),new BasicDBObject('sfz_pic',0),SJ_DESC)
    }

    def apply_handle(HttpServletRequest req)
    {
        def status = req.getInt('status')
        if (status == ApplyType.通过.ordinal() || status == ApplyType.未通过.ordinal()){

            Long time = System.currentTimeMillis()
            def record =  table().findAndModify(new BasicDBObject(_id:req[_id],status:ApplyType.未处理.ordinal()),
                    new BasicDBObject('$set':[status:status,lastmodif:time]))
            if (record ){
                def user_id = record.get('user_id') as Integer
                def name = record.get('name') as String
                def greetings = record.get('greetings') as String
                def family_pic = record.get('family_pic') as String
                def family_id = record.get('family_id') as Integer
                if (status == ApplyType.通过.ordinal()){
                    rooms().update($$(_id,family_id),$$($set:[xy_star_id:null,live:Boolean.FALSE, bean : 0, visiter_count:0,found_time:time,pic_url:family_pic,greetings:greetings,
                                                            room_ids:family_id.toString(),name: name,family_id:family_id,type: RoomType.家族.ordinal()]),true, false, writeConcern)

                }
                Crud.opLog(OpType.family_apply_handle,[user_id:user_id,family_id:family_id,status:status])
            }
        }
        OK()
    }

    def show(HttpServletRequest req){
        table().findOne(req[_id])
    }

}
