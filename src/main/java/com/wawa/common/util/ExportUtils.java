package com.wawa.common.util;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import static com.wawa.model.ExportType.ExportItem;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Administrator on 2014/9/25.
 */
public class ExportUtils {

    public static final Charset GBK = Charset.forName("GBK");

    public static final String ls = System.lineSeparator();

    private static final Long halfMonthTimeMilli = 31 * 24 * 3600 * 1000L;

    private static final int BATCH_SIZE = 50000;//默认单页查询最大的条数

    public static Map list(HttpServletRequest req, DBCollection table, DBObject query, DBObject field, DBObject sort, Closure closureRenderList) {
        int p = WebUtils.getPage(req);
        int size = BATCH_SIZE;
        Pager pager = WebUtils.mongoPager(table, query, field, sort, p, size);
        if (closureRenderList != null) {
            List list = (List) pager.getData();
            if (list.size() > 0)
                closureRenderList.call(list);
            if (pager.getAllPage() > 1) {
                while ((++p) <= pager.getAllPage()) {
                    List data = table.find(query, field).sort(sort).skip((p - 1) * size).limit(size).toArray();
                    closureRenderList.call(data);
                }
            }
        }
        return WebUtils.normalOutPager(pager);
    }

    public static StringBuffer generateTitle(Date[] dates, String count, String page) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        StringBuffer buf = new StringBuffer("当前查询日期从：").append(sdf.format(dates[0]))
                .append("到：").append(sdf.format(dates[1])).append(ls);
        //获得值列表
        if (StringUtils.isNotBlank(count))
            buf.append("共统计：").append(count).append(ls);
        return buf;
    }

    public static String generateFilename(Date[] dates, String filename) {
        if (StringUtils.isNotBlank(filename)) {
            filename = filename + new SimpleDateFormat("yyyyMMdd").format(dates[0]) +
                    "-" + new SimpleDateFormat("yyyyMMdd").format(dates[1]);
        } else {
            filename = String.valueOf(System.currentTimeMillis());
        }
        try {
            filename = URLEncoder.encode(filename, "UTF-8");
        } catch (Exception e) {

        }
        return filename;
    }

    public static StringBuffer render(List<BasicDBObject> data, List<ExportItem> list, StringBuffer buf, Boolean title) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        if (data == null || list == null) return buf;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        if (Boolean.TRUE.equals(title)) {
            for (ExportItem obj : list) {
                buf.append(",").append(obj.getValue());
            }
            buf.replace(0, 1, "");
            buf.append(ExportUtils.ls);
        }
        for (BasicDBObject obj : data) {
            Boolean first = Boolean.TRUE;
            for (ExportItem item : list) {
                String keys = item.getId();
                Object value = null;
                String key = null;
                String[] keyValue = keys.split("\\|");
                for (String k : keyValue) {
                    key = k;
                    String[] keyItem = key.split("\\.");
                    for (int i = 0; i < keyItem.length; i++) {
                        if (i == 0) {
                            value = obj.get(keyItem[i]);
                        } else {
                            if (value == null) {
                                break;
                            }
                            Map<String, Object> map = (Map<String, Object>) value;
                            value = map.get(keyItem[i]);
                        }
                    }
                    if (value != null) {
                        break;
                    }
                }
                //得到value值写入buf
                if (!first) {
                    buf.append(",");
                } else {
                    first = Boolean.FALSE;
                }
                if (value != null && (key.equals("timestamp") || key.equals("star1.bonus_time"))) {
                    value = sdf.format(new Date((Long) value));
                }
                value = (value == null) ? "" : String.valueOf(value);
                buf.append(String.valueOf(value).replaceAll("\\n", ""));
            }
            buf.append(ExportUtils.ls);
        }
        return buf;
    }

    /**
     * 自动设置查询日期，通常最新的统计信息为前一天的数据，
     * 若无etime或etime大于前一天时间则设置etime为前一天结束时间
     */
    public static Date[] checkDate(Date stime, Date etime) {
        Long endMilli = 0L;
        Long lastTimeMilli = getLastTime();
        if (etime == null || (etime.getTime() > lastTimeMilli)) {
            etime = new Date(lastTimeMilli);
        }
        endMilli = etime.getTime();
        Long startMilli = 0L;
        if (stime == null) {
            startMilli = endMilli - halfMonthTimeMilli + 1;
        } else {
            startMilli = stime.getTime();
            if ((endMilli - startMilli) > halfMonthTimeMilli) {
                startMilli = endMilli - halfMonthTimeMilli + 1;
            }
        }
        stime = new Date(startMilli);
        return new Date[]{stime, etime};
    }

    private static Long getLastTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MILLISECOND, -1);
        return cal.getTimeInMillis();
    }

    public static void response(HttpServletResponse res, String fileName, String str) {
        if (str == null) {
            str = "";
        }
        byte[] b = str.getBytes(GBK);
        res.addHeader("Content-Disposition", "attachment;filename=" + fileName + ".csv");
        res.addHeader("Content-Length", b.length + "");
        res.setContentType("application/octet-stream;charset=" + GBK.toString());
        res.setCharacterEncoding(GBK.toString());
        ServletOutputStream out = null;
        byte[] temp = new byte[2048];
        int start = 0;
        int length = temp.length < b.length ? temp.length : b.length;
        try {
            while (length > 0) {
                System.arraycopy(b, start, temp, 0, length);
                out = res.getOutputStream();
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

    public static void response(HttpServletResponse res, String fileName, File str) {
        if (str == null) {
            return;
        }
        res.addHeader("Content-Disposition", "attachment;filename=" + fileName + ".txt");
        res.addHeader("Content-Length", str.length() + "");
        res.setContentType("application/octet-stream;charset=" + GBK.toString());
        res.setCharacterEncoding(GBK.toString());
        InputStream in = null;
        ServletOutputStream out = null;
        byte[] temp = new byte[2048];
        try {
            in = new FileInputStream(str);
            out = res.getOutputStream();
            int len;
            while ((len = in.read(temp, 0, temp.length)) != -1) {
                out.write(temp, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
