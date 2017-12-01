package com.ttpod.star.admin;

import com.mongodb.*;
import com.ttpod.rest.AppProperties;
import com.ttpod.rest.common.doc.MongoKey;
import com.ttpod.rest.common.util.JSONUtil;
import com.ttpod.rest.persistent.KGS;
import com.ttpod.rest.web.Crud;
import com.ttpod.rest.web.StaticSpring;
import com.ttpod.rest.web.support.FreemarkerSupport;
import com.ttpod.rest.web.support.FreemarkerSupport7;
import com.ttpod.star.common.util.KeyUtils;
import groovy.transform.CompileStatic;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static com.ttpod.rest.common.doc.MongoKey.*;
import static com.ttpod.rest.common.doc.MongoKey._id;

/**
 * action<br>
 *
 *
 * date: 12-8-20 下午2:58
 *
 * @author: yangyang.cong@ttpod.com
 */
@CompileStatic
public abstract class BaseController extends FreemarkerSupport7 {

    @Resource
    public KGS seqKGS;

    @Resource
    public MongoTemplate mainMongo;
    @Resource
    public MongoTemplate rankMongo;
//    @Resource
//    public MongoTemplate gameLogMongo;

//    @Resource
//    public  MongoTemplate unionMongo;

    public static final MongoTemplate logMongo = (MongoTemplate) StaticSpring.get("logMongo");
    public static final MongoTemplate unionMongo = (MongoTemplate) StaticSpring.get("unionMongo");

    public static final MongoTemplate adminMongo = (MongoTemplate) StaticSpring.get("adminMongo");

    public static final MongoTemplate topicMongo = (MongoTemplate) StaticSpring.get("topicMongo");

    public static final MongoTemplate activeMongo = (MongoTemplate) StaticSpring.get("activeMongo");

    public static final MongoTemplate familyMongo = (MongoTemplate) StaticSpring.get("familyMongo");
    public static final MongoTemplate shopMongo = (MongoTemplate) StaticSpring.get("shopMongo");
    public static final MongoTemplate gameLogMongo = (MongoTemplate) StaticSpring.get("gameLogMongo");
    public static final MongoTemplate catchMongo = (MongoTemplate) StaticSpring.get("catchMongo");

    //是否为测试服
    public static final boolean isTest  = AppProperties.get("api.domain").contains("test-");

    @Resource
    public StringRedisTemplate mainRedis;

    @Resource
    public StringRedisTemplate chatRedis;

    @Resource
    public StringRedisTemplate userRedis;


    @Resource
    public WriteConcern writeConcern;

    public DBCollection users(){return mainMongo.getCollection("users");}
    public DBCollection rooms(){return mainMongo.getCollection("rooms");}

    public DBCollection table(){
        throw new UnsupportedOperationException("One shuold overried this");
    }

    public Map list(HttpServletRequest req,DBObject query){
        return Crud.list(req, table(), query, ALL_FIELD, MongoKey.SJ_DESC);
    }

    private static final Map OK = new HashMap();
    static {
        OK.put("code",1);
    }
    public Map OK(){
        return OK;
    }

    public boolean intQuery(QueryBuilder q,HttpServletRequest req,String field){
        String str = req.getParameter(field);
        if (StringUtils.isNotBlank(str)){
            q.and(field).is(Integer.valueOf(str));
            return true;
        }
        return false;
    }

    public boolean booleanQuery(QueryBuilder q,HttpServletRequest req,String field){
        String str = req.getParameter(field);
        if (StringUtils.isNotBlank(str)){
            q.and(field).is(str.equals("1"));
            return true;
        }
        return false;
    }

    public boolean stringQuery(QueryBuilder q,HttpServletRequest req,String field){
        String  str = req.getParameter(field);
        if (StringUtils.isNotBlank(str)){
            q.and(field).is(str);
            return true;
        }
        return false;
    }
    public static final String timestamp = MongoKey.timestamp;
    public static final String stime = "stime";
    public static final String _id = MongoKey._id;
    static final String finance_coin_count = "finance.coin_count";
    static final String finance_diamond_count = "finance.diamond_count";
    static final String finance_coin_spend_total = "finance.coin_spend_total";
    static final String finance_log="finance_log";
    static final String finance_log_id=finance_log+"."+_id;



    public boolean addDiamond(Integer userId, Long diamond, BasicDBObject logWithId)
    {
        BasicDBObject obj = new BasicDBObject(finance_diamond_count,diamond);
        return  this.addCoin(userId,diamond,logWithId,obj);
    }


    public boolean refundCoin(Integer userId,Long coin,BasicDBObject logWithId)
    {
        BasicDBObject obj =  new BasicDBObject(finance_coin_count,coin).append(finance_coin_spend_total,-coin) ;
        return   this.addCoin(userId,coin,logWithId,obj);
    }


    private boolean addCoin(Integer userId,Long coin,BasicDBObject logWithId,BasicDBObject obj){
        String log_id = (String) logWithId.get("_id");
        if( coin < 0 || log_id == null ){
            return false;
        }
        if(logWithId.get("to_id") == null){
            logWithId.put("to_id", userId);
        }
        if(logWithId.get(timestamp) == null){
            logWithId.put(timestamp, System.currentTimeMillis());
        }
        DBCollection users = users();
        DBObject my_user =  users.findOne(new BasicDBObject(_id, userId)) ;
        if(my_user!= null)
        {
            if(null !=  my_user.get("qd"))
            {
                String qd = my_user.get("qd").toString() ;
                logWithId.append("qd",qd) ;
            }
        }
        DBCollection logColl = adminMongo.getCollection(finance_log);
        if(logColl.count(new BasicDBObject(_id,log_id)) == 0  &&
                users.update(new BasicDBObject(_id,userId).append(finance_log_id,new BasicDBObject($ne,log_id)),
                        new BasicDBObject($inc, obj)
                                .append($push,new BasicDBObject(finance_log,logWithId)),
                        false,false,writeConcern
                ).getN() == 1){

            logColl.save(logWithId,writeConcern);
            users.update(new BasicDBObject(_id, userId),
                    new BasicDBObject($pull,new BasicDBObject(finance_log,new BasicDBObject(_id,log_id))),
                    false,false,writeConcern);

            return true;
        }
        return false;
    }




    public static final String auth_code="auth_code";
    /**
     * 检查验证码是否输错
     */
    public boolean codeVerifError(HttpServletRequest req,String input){
        HttpSession s=  req.getSession();
        String server = (String) s.getAttribute(auth_code);
        if(input == null ||  !input.equalsIgnoreCase(server) ){
            return true;
        }
        s.removeAttribute(auth_code);
        return false;
    }

    public void publish(final String channel , final String json){
        StaticSpring.execute(
                new Runnable() {
                    public void run() {
                        final byte[] data = KeyUtils.serializer(json);
                        chatRedis.execute(new RedisCallback() {
                            @Override
                            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                                return connection.publish(KeyUtils.serializer(channel), data);
                            }
                        });
                    }
                }
        );
    }
    public void publish(final String channel , Map<String,Object> json){
        publish(channel, JSONUtil.beanToJson(json));
    }


}


