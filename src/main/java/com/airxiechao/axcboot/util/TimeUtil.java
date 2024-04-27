package com.airxiechao.axcboot.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

    public static String toStr(Date date, String format){
        if(null == date){
            return null;
        }

        SimpleDateFormat fmt = new SimpleDateFormat(format);
        return fmt.format(date);
    }

    public static Date fromStr(String time, String format) throws ParseException {
        if(null == time || time.isBlank()){
            return null;
        }

        SimpleDateFormat fmt = new SimpleDateFormat(format);
        return fmt.parse(time);
    }

    public static String toTimeStr(Date date){
        if(null == date){
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    public static String toDateStr(Date date){
        if(null == date){
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(date);
    }

    public static Date toTime(String time) throws ParseException {
        if(null == time || time.isBlank()){
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.parse(time);
    }

    public static Date toDate(String date) throws ParseException {
        if(null == date || date.isBlank()){
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.parse(date);
    }

    public static Date toNowDate() throws ParseException {
        return toDate(toDateStr(new Date()));
    }

    public static Date toNowTime() throws ParseException {
        return toTime(toTimeStr(new Date()));
    }

    public static Date clearTime(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public static Date tomorrow(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, 1);

        return cal.getTime();
    }

    public static Date yesterday(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1);

        return cal.getTime();
    }

    /**
     * 计算时间差的文本
     * @param from
     * @param to
     * @return
     */
    public static String timeDiffText(Date from, Date to){
        long diff = to.getTime() - from.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        long hours = diff / (1000 * 60 * 60) % 24;
        long minutes = diff / (1000 * 60) % 60;
        if(diff % 1000 > 0){
            minutes += 1;
        }

        StringBuilder sb = new StringBuilder();
        if(days > 0){
            sb.append(days+"天");
        }
        if(days > 0 || hours > 0){
            sb.append(hours+"小时");
        }
        if(days > 0 || hours > 0 || minutes > 0){
            sb.append(minutes+"分钟");
        }

        return sb.toString();
    }

    /**
     * 计算分钟数差
     * @param from
     * @param to
     * @return
     */
    public static int diffMinute(Date from, Date to){
        return (int)((to.getTime() - from.getTime()) / (1000 * 60));
    }

    /**
     * 计算天数差
     * @param from
     * @param to
     * @return
     */
    public static int diffDay(Date from, Date to){
        long timeFrom = from.getTime();
        long timeTo = to.getTime();

        long diffDay = (timeTo - timeFrom) / (1000 * 60 * 60 * 24);

        return (int)diffDay;
    }

    /**
     * 计算相差月数
     * @param from
     * @param to
     * @return
     */
    public static int diffMonth(Date from, Date to){
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.setTime(from);

        Calendar toCalendar = Calendar.getInstance();
        toCalendar.setTime(to);

        int diffYear = toCalendar.get(Calendar.YEAR) - fromCalendar.get(Calendar.YEAR);
        int diffMonth = diffYear * 12 + toCalendar.get(Calendar.MONTH) - fromCalendar.get(Calendar.MONTH);

        return diffMonth;
    }

    /**
     * 加月数
     * @param from
     * @param month
     * @return
     */
    public static Date addMonth(Date from, int month){
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.setTime(from);

        fromCalendar.add(Calendar.MONTH, month);

        return fromCalendar.getTime();
    }

    /**
     * 加天数
     * @param from
     * @param day
     * @return
     */
    public static Date addDay(Date from, int day){
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.setTime(from);

        fromCalendar.add(Calendar.DAY_OF_YEAR, day);

        return fromCalendar.getTime();
    }

    /**
     * 加分钟数
     * @param from
     * @param minute
     * @return
     */
    public static Date addMinute(Date from, int minute){
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.setTime(from);

        fromCalendar.add(Calendar.MINUTE, minute);

        return fromCalendar.getTime();
    }

    /**
     * 加秒数
     * @param from
     * @param secend
     * @return
     */
    public static Date addSecond(Date from, int secend){
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.setTime(from);

        fromCalendar.add(Calendar.SECOND, secend);

        return fromCalendar.getTime();
    }
}
