package com.ttpod.groovy

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.web.Crud
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

import static com.ttpod.groovy.CrudExtClosures.IntNullable
import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey._id

/**
 * Created by Administrator on 2017/12/6.
 */
@CompileStatic
public final class CrudExt {

    private DBCollection table
    private Boolean intId
    private Map<String, Closure> props

    public CrudExt(DBCollection table, Map<String, Closure> props) {
        this.table = table
        intId = false
        this.props = props
    }

    public CrudExt(DBCollection table, Map<String, Closure> props, boolean intId) {
        this.table = table
        this.intId = intId
        this.props = props
    }

    public Map add(HttpServletRequest req) {

        Map map = new HashMap();
        for (Map.Entry<String, Closure> entry : props.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue().call(req.getParameter(key));
            if (val != null && val != CrudExtClosures.NULL) {
                map.put(key, val)
            }
        }
        if(table.save(new BasicDBObject(map)).getN() == 1){
            Crud.opLog(table.getName() + "_add", map);
        }

        return IMessageCode.OK
    }

    public Object edit(HttpServletRequest req) {

        Object id = parseId(req)
        if(null == id){
            return IMessageCode.CODE0
        }

        Map map = new HashMap()
        Map unset = new HashMap()
        for (Map.Entry<String, Closure> entry : props.entrySet()) {
            String key = entry.getKey()
            if(key.equals(_id)){
                continue
            }
            String strValue = req.getParameter(key)
            if(strValue != null){
                Object val = entry.getValue().call(strValue)
                if (val != null) {
                    if (CrudExtClosures.NULL == val) {
                        unset.put(key, 1)
                    } else {
                        map.put(key, val)
                    }
                }
            }
        }
        def update = new BasicDBObject()
        if (!map.isEmpty()) {
            map.put("lastModif", System.currentTimeMillis())
            update.put('$set', map)
        }
        if (!unset.isEmpty()) {
            update.put('$unset', unset)
        }
        if(map.size() > 0 && table.update(new BasicDBObject(_id,id), update).getN() == 1){
            map.put(_id,id)
            Crud.opLog(table.getName() + "_edit", [set: map, unset: unset])
        }
        return IMessageCode.OK
    }

    public Object parseId(HttpServletRequest req) {
        String id = req.getParameter(_id)
        if (id == null || id.isEmpty()) {
            return null
        }
        return intId ? Integer.valueOf(id) : id
    }

}
