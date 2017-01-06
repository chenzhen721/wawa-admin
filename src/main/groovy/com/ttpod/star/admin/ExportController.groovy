package com.ttpod.star.admin

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.ttpod.rest.anno.RestWithSession
import com.ttpod.rest.web.StaticSpring
import com.ttpod.star.common.util.ExportUtils
import com.ttpod.star.model.ExportType
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat

import static com.ttpod.rest.common.doc.MongoKey.ALL_FIELD
import static com.ttpod.rest.common.doc.MongoKey.NATURAL_DESC

/**
 * Created by Administrator on 2014/11/12.
 */
@RestWithSession
class ExportController extends BaseController {

    public static final MongoTemplate historyMongo = (MongoTemplate) StaticSpring.get("historyMongo");

    private static final String SUFFIX = ".txt"

    DBCollection table() { historyMongo.getCollection("room_cost_2015") }

    File static_folder

    @Value("#{application['static.folder']}")
    void setStaticFolder(String folder) {
        static_folder = new File(folder)
        static_folder.mkdirs()
        println "初始化统计上传目录 : ${folder}"
    }

    /**
     * 只能查询一个月前的记录,当月记录使用报表导出功能查询
     * 生成送礼流水文件(根据文件名修改下载状态)
     */
    def send_gift_history(HttpServletRequest req, HttpServletResponse res) {
        def update = req.getParameter('update') as Integer
        //时间固定按月统计
        Date[] dates = checkDate(Web.getStime(req))
        if (dates == null) {
            return [code: 0, msg: "时间参数无效"]
        }
        def type = 'send_gift'
        def typeName = type;
        String filename = ExportUtils.generateFilename(dates, "${typeName}")
        String name = ""
        try {
            name = URLDecoder.decode(filename, "UTF-8");
        } catch (Exception e) {
            return [code: 0, msg: "文件名无效"]
        }
        File file = new File(static_folder.getPath() + '/' + name + SUFFIX)
        if (file.exists() && update == 1) {
            file.delete()
        }
        if (!file.exists()) {
            File executeFile = new File(static_folder.getPath() + '/' + name + "executing" + SUFFIX)
            if (executeFile.exists()) {
                if (update == 1) {
                    executeFile.delete()
                } else {
                    return [code: 0, msg: "文件正在生成中，请稍后再试"]
                }
            }
            executeFile.createNewFile()
            StaticSpring.execute(new Runnable() {
                private Boolean title = Boolean.TRUE
                private Long start = System.currentTimeMillis()

                @Override
                void run() {
                    def query = Web.fillTimeBetween(dates[0], dates[1]).and('type').is(type)
                    def result = ExportUtils.list(req, historyMongo.getCollection("room_cost_2015"), query.get(), ALL_FIELD, NATURAL_DESC) { List<BasicDBObject> list ->
                        def bodyBuf = new StringBuffer()
                        ExportUtils.render(list, ExportType.getListByType(type), bodyBuf, title)
                        if (Boolean.TRUE.equals(title)) title = Boolean.FALSE
                        write(executeFile, bodyBuf.toString())
                    } as Map
                    //def count = result['count'] as String
                    //def page = result['all_page'] as String
                    //StringBuffer titleBuf = ExportUtils.generateTitle(dates, count, page)
                    executeFile.renameTo(file)
                    println "文件生成完成，用时：${System.currentTimeMillis() - start}".toString()
                }

                private void write(File f, String content) {
                    byte[] b = content.getBytes(ExportUtils.GBK);
                    byte[] temp = new byte[2048];
                    int start = 0;
                    int length = temp.length < b.length ? temp.length : b.length;
                    OutputStream out = null
                    try {
                        out = new FileOutputStream(f, true)
                        while (length > 0) {
                            System.arraycopy(b, start, temp, 0, length);
                            out.write(temp, 0, length);
                            start += length;
                            length = (temp.length <= b.length - start) ? temp.length : (b.length - start);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (out != null) {
                                out.flush();
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            })
            return [code: 0, msg: "文件正在生成中，请稍后再试"]
        } else {
            ExportUtils.response(res, filename, file)
        }
    }

    private Date[] checkDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM")
        Calendar cal = Calendar.getInstance()//获取当前日期
        Date cur = cal.getTime()
        if (date != null) {
            cal.setTime(date)
        } else {
            cal.add(Calendar.MONTH, -1)
        }
        if (sdf.format(cur).equals(cal.getTime())) {
            return null
        }
        cal.set(Calendar.DAY_OF_MONTH, 1)//设置为1号,当前日期对应的当月第一天
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        Date stime = cal.getTime()
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        Date etime = cal.getTime()
        return [stime, etime] as Date[]
    }

}
