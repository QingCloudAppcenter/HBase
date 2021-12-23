package com.qingcloud.appcenter.hbase.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * @author appcenter
 */
public class Constant {
    private static final Logger logger = LoggerFactory.getLogger(Constant.class);

    public static String Action = "";

    public static String HBaseConfPath = "/opt/hbase/conf/hbase-site.xml";

    // Create table
    public static String TableName ;
    public static String[] ColumnFamilies = {"base_info"};
    public static String[] ColumnFamily = {"base_info"};

    // write data
    public static long RowNum = 1000000;
    public static long BatchNum = 500000;
    public static String DefaultStartRowKey = "1";
    // start rowkey of generate
    public static String StartRowKey = null;
    // end rowkey of generate
    public static String EndRowKey = null;

    // write wide table data
    // write WideTableColumnNum columns when rowkey equals to WideTableRowKey
    public static long WideTableRowKey;
    public static long WideTableColumnNum;

    public static boolean Help = false;

    // read setting
    public static long MaxScanBatchNum = 100000;
    public static long ScanBatchNum = 0L;

    public static ArrayList<String> WithoutValueFlags = new ArrayList<>();
    //设置日期格式
    public static SimpleDateFormat DateFormatCons = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final NumberFormat NF = NumberFormat.getNumberInstance();

    public static void init(String[] args) {
        WithoutValueFlags.add("-h");
        WithoutValueFlags.add("--help");

        // parse command params
        String flag = "";
        String value = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                flag = arg;
            } else {
                value = arg;
            }

            if (!WithoutValueFlags.contains(flag) && value == null) {
                continue; // to get value
            }

            switch (flag) {
                case "-a":
                    Action = value;
                    break;
                case "-f":
                    HBaseConfPath = value;
                    break;
                case "-t":
                    TableName = value;
                    break;
                case "-c":
                    ColumnFamilies = value.split(",");
                    break;
                case "-s":
                    StartRowKey = value;
                    break;
                case "-e":
                    EndRowKey = value;
                    break;
                case "-n":
                    RowNum = Long.parseLong(value);
                    break;
                case "-b":
                    BatchNum = Long.parseLong(value);
                    ScanBatchNum = Long.parseLong(value);
                    break;
                case "-r":
                    WideTableRowKey = Long.parseLong(value);
                    break;
                case "-w":
                    WideTableColumnNum = Long.parseLong(value);
                    break;
                case "-h":
                case "-help":
                    Help = true;
                    break;
            }
            value = null;
        }

        if (!Help){
            // check required
            if (isEmpty(TableName)) {
                logger.error("Required table name is null!");
                System.exit(1);
            }

            logger.debug("Action: {}", Action);
            logger.debug("HbaseConfPath: {}", HBaseConfPath);
            logger.debug("TableName: {}", TableName);
            logger.debug("ColumnFamilies: {}", ColumnFamilies.toString());
            logger.debug("StartRowKey: {}", StartRowKey);
            logger.debug("RowNum: {}", RowNum);
            logger.debug("BatchNum: {}", BatchNum);
            logger.debug("WideTableRowKey: {}", WideTableRowKey);
            logger.debug("WideTableColumnNum: {}", WideTableColumnNum);
        }
    }

    public static boolean isEmpty(String s) {
        if (s == null || "".equals(s)){
            return true;
        }
        return false;
    }
}
