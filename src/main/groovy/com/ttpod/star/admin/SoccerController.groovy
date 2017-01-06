package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.doc.IMessageCode
import com.ttpod.rest.common.doc.MongoKey
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.Crud
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.common.util.KeyUtils
import com.ttpod.star.model.OpType
import com.ttpod.star.model.PayType
import com.ttpod.star.model.User
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

import static com.ttpod.rest.common.doc.MongoKey.*
import static com.ttpod.rest.common.util.WebUtils.$$
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * date: 14-3-24 下午2:31
 * @author: jiao.li@ttpod.com
 */
//@Rest
@RestWithSession
class SoccerController extends BaseController{

    /*
      巴西,喀麦隆,墨西哥,克罗地亚,
      西班牙,智利,澳大利亚,荷兰,
      哥伦比亚,科特迪瓦,日本,希腊,
      乌拉圭,英格兰,哥斯达黎加,意大利,
      瑞士,厄瓜多尔,洪都拉斯,法国,
      阿根廷,尼日利亚,伊朗,波黑,
      ,德国,加纳,美国,葡萄牙,
      ,比利时,阿尔及利亚,韩国,俄罗斯
     */
    private static final Map<String, String> TEAMS = ['1':'巴西','2':'喀麦隆','3':'墨西哥','4':'克罗地亚',
                                                    '5':'西班牙','6':'智利','7':'澳大利亚','8':'荷兰',
                                                    '9':'哥伦比亚','10':'科特迪瓦','11':'日本','12':'希腊',
                                                    '13':'乌拉圭','14':'英格兰','15':'哥斯达黎加','16':'意大利',
                                                    '17':'瑞士','18':'厄瓜多尔','19':'洪都拉斯','20':'法国',
                                                    '21':'阿根廷','22':'尼日利亚','23':'伊朗','24':'波黑',
                                                    '25':'德国','26':'加纳','27':'美国','28':'葡萄牙',
                                                    '29':'比利时','30':'阿尔及利亚','31':'韩国','32':'俄罗斯']

    private static final Integer[] GIFTS = [0, 23, 21, 225]

    private static final Integer[] TEAM_SIZE = [0, 3, 4, 8]

    static final String DFMD = "yyyy-MM-dd HH:mm:ss";

    def teams(HttpServletRequest req){
        [code:1, data:TEAMS]
    }

    def list(HttpServletRequest req){
        def q = Web.fillTimeBetween(req)
        q.and('type').is(req['type'] as Integer)
        Crud.list(req,activeMongo.getCollection("soccer_match"), q.get(), null, $$(timestamp : -1))
    }

    def log_list(HttpServletRequest req){
        def q = Web.fillTimeBetween(req)
        if(req['type'] != null)
            q.and("type").is(req['type'] as Integer);
        if(req['userId'] != null)
            q.and("userId").is(req['userId'] as Integer);
        Crud.list(req,activeMongo.getCollection("soccer_logs"), q.get(), null, $$(timestamp : -1))
    }
    /**
     * cat : 1 胜负平  2：胜负
     * status : 0:未开奖 1:开奖
     * 每日竞猜
     * @param req
     */
    def add(HttpServletRequest req){
        def date = req['date']
        def teamA = req['teamA']
        def teamB = req['teamB']
        def cat = req['cat']
        String match = "${Date.parse("yyyy-MM-dd",date).format("MM-dd")} ${TEAMS[teamA]} VS ${TEAMS[teamB]}"
        def _id = System.currentTimeMillis();
        Crud.opLog('worldcup_add',[rid:_id])
        [code : activeMongo.getCollection("soccer_match").save($$(_id: _id,
                teamA:teamA,
                teamB:teamB,
                cat:cat,
                status:0,
                A:0, B:0,C:0,
                coins:0,
                date:date,
                match:match,
                type : 4,
                timestamp:System.currentTimeMillis()
        ), writeConcern).getN()]
    }

    def del(HttpServletRequest req){
        Long rid = req['rid'] as Long
        Crud.opLog('worldcup_del',[rid:rid])
        [code : activeMongo.getCollection("soccer_match").remove($$(_id, rid), writeConcern).getN()]
    }
    /**
     * result A B C
     * 公布结果每日竞猜
     * @param req
     */
    def publishDaily(HttpServletRequest req){
        final rid = req['rid'] as Long
        final result = req['result'].toUpperCase()

        //计算奖金额度
        def round = activeMongo.getCollection("soccer_match").findAndModify($$(_id: rid, status:0), $$($set:$$(status:1,result:result)))
        if(round == null) return [code:0]

        //比赛结果
        List teamsLst = new ArrayList(1);
        if(result.equals("A")){
            teamsLst.add(TEAMS[round['teamA'] as String]+"获胜")
        }else if(result.equals("B")){
            teamsLst.add(TEAMS[round['teamB'] as String]+"获胜")
        }else{
            teamsLst.add("双方打平");
        }
        if(teamsLst.size() <= 0) return [code:0]

        //猜中人数
        def win_count = round[result] as Integer
        if(win_count == null || win_count <= 0) return [code : 0]

        activeMongo.getCollection("soccer_match").update($$(_id: rid, status:1),
                $$($set:$$(win:teamsLst.get(0), win_count:win_count)))

        Long coins = round['coins'] as Long
        def award_coin = coins/win_count

        def lotteryDB= logMongo.getCollection("lottery_logs")

        StaticSpring.execute(
                new Runnable() {
                    public void run() {
                        Integer award_count = 0
                        def query = $$("result":rid+"_"+result, type: 4,status:0)
                        def count = activeMongo.getCollection("soccer_logs").count(query)
                        def size = 5000
                        def allPage = (int)((count + size - 1) / size);
                        Long l = System.currentTimeMillis()
                        def users = users()
                        while(allPage > 0){
                            List lottery_lst = new ArrayList(5000);
                            activeMongo.getCollection("soccer_logs").find(query, $$(_id : 1, 'count':1, userId : 1)).skip((allPage - 1) * size)
                                    .limit(size).toArray().each {DBObject dbo ->
                                Long coin = (award_coin * (dbo.get("count") as Integer)) as Long
                                if(activeMongo.getCollection("soccer_logs").update($$(_id: dbo.get("_id") as String, status:0),
                                        $$($set, $$(status:1,award:coin)),
                                        false, false, writeConcern).getN() == 1){
                                    if(1 ==  users.update($$(_id,dbo.get("userId") as Integer),
                                            $$($inc,$$("finance.coin_count",coin))).getN()){
                                        //获奖日志
                                        lottery_lst.add(award(dbo.get("userId") as Integer, coin, dbo.get("_id") as String))
                                    }
                                }
                                award_count++;
                            }
                            lotteryDB.insert(lottery_lst)
                            allPage--
                            Thread.sleep(500)
                        }
                        activeMongo.getCollection("soccer_logs").updateMulti($$(rid:rid, type : 4), $$($set, $$(status:1,wins:teamsLst)))
                        println "publishDaily cost time: " + (System.currentTimeMillis() - l)
                        println "publishDaily award_count: " + award_count
                    }

                    def BasicDBObject award(Integer userId, Long coin, String id){
                        String _id = "${id}_${userId}_${System.currentTimeMillis()}_worldcup".toString()
                        def lottery = new BasicDBObject(
                                _id : _id,
                                total_coin:coins,
                                type:'lottery',
                                active_name: "worldcup",
                                user_id:userId,
                                award_coin:coin,
                                timestamp:System.currentTimeMillis())
                        return lottery
                    }

                }
        )
        Crud.opLog('worldcup_publishDaily',[rid:rid])
        return [code:1]
    }

    /**
     * 公布结果
     * type 1: 前三名  2：四强   3: 八强
     * @param req
     */
    def publish(HttpServletRequest req){
        //前三名顺序
        Integer type = req['type'] as Integer
        if(type == null) return [code:0]
        String[] teams = new String[3]
        if(type == 1){
            teams[0] =  req.getParameter("team1")
            teams[1] =  req.getParameter("team2")
            teams[2] =  req.getParameter("team3")
        }
        //四强 八强
        else if(type == 2 || type == 3){
            teams = StringUtils.split(req["teams"], ",")
            Arrays.sort(teams)
        }
        if(teams == null || teams.size() != TEAM_SIZE[type] || isRepeat(teams))  return [code:0]
        award_process(teams, type)
        Crud.opLog('worldcup_publish',[type:type])
        return [code:1]
    }

    private Boolean isRepeat(String[] teams){
        Set set = new HashSet();
        set.addAll(teams)
        return  set.size() != teams.size()
    }

    def award_process(final String[] teams, final Integer type){
        def lotteryDB= logMongo.getCollection("lottery_logs")
        String teamstr = StringUtils.join(teams, "_")
        List teamsLst = new ArrayList(10);
        teams.each {
            if(TEAMS.containsKey(it as String))
                teamsLst.add(TEAMS.get(it as String))
        }
        activeMongo.getCollection("soccer_match").save($$(_id: "Guess_"+type,
                teams:teams,
                teamsLst:teamsLst,
                type:type,
                timestamp:System.currentTimeMillis()))

        StaticSpring.execute(
                new Runnable() {
                    public void run() {
                        Integer award_count = 0
                        def query = $$("result":teamstr, type:type,status:0)
                        def count = activeMongo.getCollection("soccer_logs").count(query)
                        def size = 5000
                        def allPage = (int)((count + size - 1) / size);
                        Long l = System.currentTimeMillis()
                        def users = users()
                        while(allPage > 0){
                            List lottery_lst = new ArrayList(5000);
                            activeMongo.getCollection("soccer_logs").find(query, $$(_id : 1, 'count':1, userId : 1)).skip((allPage - 1) * size)
                                    .limit(size).toArray().each {DBObject dbo ->
                                if(activeMongo.getCollection("soccer_logs").update($$(_id: dbo.get("_id") as String, status:0),
                                        $$($set, $$(status:1,award:GIFTS[type])),
                                        false, false, writeConcern).getN() == 1){
                                    Integer userId = dbo.get("userId") as Integer
                                    Integer gift_count = dbo.get("count") as Integer
                                    if(1 ==  users.update($$(_id, userId),$$($inc,$$("bag."+GIFTS[type], gift_count))).getN()){
                                        //获奖日志
                                        lottery_lst.add(award(userId, dbo.get("_id") as String))
                                        Web.obtainGift(userId, GIFTS[type], gift_count);
                                    }

                                }
                                award_count++;
                            }
                            lotteryDB.insert(lottery_lst)
                            allPage--
                            Thread.sleep(500)
                        }
                        activeMongo.getCollection("soccer_logs").updateMulti($$(type:type), $$($set, $$(status:1,wins:teamsLst)))
                        println "award_process cost time: " + (System.currentTimeMillis() - l)
                        println "award_process award_count: " + award_count
                    }

                    def BasicDBObject award(Integer userId, String id){
                        String _id = "${id}_${type}_worldcup".toString()
                        def lottery = new BasicDBObject(
                                _id : _id,
                                type:'lottery_'+type,
                                active_name: "worldcup",
                                user_id:userId,
                                gfit:GIFTS[type],
                                timestamp:System.currentTimeMillis())
                        return lottery
                    }

                }
        )
    }


}
