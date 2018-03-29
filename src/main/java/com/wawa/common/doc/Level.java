package com.wawa.common.doc;

import groovy.transform.CompileStatic;

/**
 * 用户等级
 * date: 13-2-28 下午5:30
 */
@CompileStatic
public class Level {



    static final int xingguang_max_level = 2000000; //家族星光等级
    static final long[] xingguang_levels = new long[xingguang_max_level];



    static final long[] starLevels = {0,
            1000,
            6000,
            17000,
            35000,
            70000,
            120000,
            180000,
            250000,
            320000,
            420000,
            550000,
            700000,
            900000,
            1100000,
            1400000,
            1700000,
            2000000,
            2300000,
            2700000,
            3100000,
            3600000,
            4200000,
            4800000,
            5400000,
            6000000,
            7000000,
            8000000,
            9000000,
            10000000,
            12000000,
            14000000,
            16000000,
            18000000,
            20000000,
            23000000,
            28000000,
            33000000,
            38000000,
            43000000,
            48000000,
            54000000,
            60000000,
            66000000,
            72000000,
            78000000,
            88000000,
            98000000,
            108000000,
            118000000,
            128000000,
            148000000,
            168000000,
            198000000,
            228000000,
            268000000};

    static final long[] userLevels = {0,
            1000,
            5000,
            15000,
            30000,
            50000,
            80000,
            150000,
            300000,
            500000,
            700000,
            1000000,
            1500000,
            2000000,
            2500000,
            3500000,
            5000000,
            7000000,
            10000000,
            15000000,
            21000000,
            28000000,
            36000000,
            45000000,
            55000000,
            70000000,
            108000000,
            168000000,
            258000000,
            458000000};

    static final String[] userLevelDesc = {"庶民","一富","二富","三富","四富","五富","六富","七富","八富","九富","十富","十富","十富"
            ,"举人","贡士","进士","知府","巡抚","总督","尚书","太傅","太师","丞相","藩王","郡王","亲王","诸侯","王爷","皇帝","大帝","玉帝","天尊"};

    static final long[] userWeekStarLevels = {0,20000,50000,100000,200000,500000};

    static final int star_max_level = starLevels.length;
    static final int user_max_level = userLevels.length;
    static final int user_weekstar_max_level = userWeekStarLevels.length;

    static int deltaCoin(int level){
        return 1000* (level * level+level-1);
    }


    /*
    static final int max_level = 200;
    static final long[] levels = new long[max_level];


    static {
        for(int i=1;i<max_level;i++){,levels[i] = levels[i-1] + deltaCoin(i);
        }

        for(int i=1;i<xingguang_max_level;i++){,xingguang_levels[i] = xingguang_levels[i-1] + deltaCoin(i);
        }
    }

    public static int starLevel(long coin){
        if(coin >=68000000){,int level = max_level;,if(coin >= 68000000),    level = 56;,if(coin >= 88000000),    level = 57;,if(coin >= 118000000),    level = 58;,if(coin >= 168000000),    level = 59;,if(coin >= 258000000),    level = 60;,return level;
        }else{,for (int i=1;i<max_level;i++){,    if(coin < levels[i]){,        return i-1;,    },}
        }
        return max_level;
    }*/

    public static int starLevel(long coin){
        for (int i=1;i<star_max_level;i++){
            if(coin < (starLevels[i])){
                return i-1;
            }
        }
        return star_max_level-1;
    }

    public static int userLevel(long coin){
        for (int i=1;i<user_max_level;i++){
            if(coin < (userLevels[i])){
                return i-1;
            }
        }
        return user_max_level-1;
    }

    //闪耀财星等级
    public static int userWeekStarLevel(long coin){
        for (int i=1;i<user_weekstar_max_level;i++){
            if(coin < (userWeekStarLevels[i])){
                return i-1;
            }
        }
        return user_weekstar_max_level-1;
    }

    public static long userSpendCoinByLevel(Integer level){
        return userLevels[level];
    }

    /**
     * 用户等级描述
     * @param level
     * @return
     */
    public static String userLevelDesc(Integer level){
        return userLevelDesc[level];
    }

    public static int xingGuangLevel(long coin){
        for (int i=1;i<xingguang_max_level;i++){if(coin < xingguang_levels[i]){return i-1;}
        }
        return xingguang_max_level;
    }


    //http://red.ttpod.com/redmine/projects/xinyuan/wiki/%E6%98%9F%E6%84%BFAPI%E6%96%87%E6%A1%A3#点歌

    public static int orderSongNeed(int level){
        if(level<=3){return 500;
        }else if (level<=9){return 1000;
        }else {return 1500;
        }
    }


    public static void main(String[] args) {

      /*  System.out.println(starLevel(898988));*/

        System.out.println(starLevel(1000));
        System.out.println(starLevel(250000));
        System.out.println(starLevel(5400000));
        System.out.println(starLevel(268000000));
        System.out.println(starLevel(268000001));

        System.out.println("user================");
        assert userLevel(0)!=1;
        assert userLevel(999)!=0;
        System.out.println(userLevel(0));
        System.out.println(userLevel(999));
        System.out.println(userLevel(1000));
        System.out.println(userLevel(700000));
        System.out.println(userLevel(700001));
        System.out.println(userLevel(5000000));
        System.out.println(userLevel(5000001));
        System.out.println(userLevel(258000000));
        System.out.println(userLevel(258000001));
        System.out.println(userLevel(458000000));
        System.out.println(userLevel(458000001));

        System.out.println("weekstar================");
        System.out.println(userWeekStarLevel(0));
        System.out.println(userWeekStarLevel(20000));
        System.out.println(userWeekStarLevel(50000));
        System.out.println(userWeekStarLevel(100000));
        System.out.println(userWeekStarLevel(200000));
        System.out.println(userWeekStarLevel(500000));
        System.out.println(userWeekStarLevel(500001));


    }

}
