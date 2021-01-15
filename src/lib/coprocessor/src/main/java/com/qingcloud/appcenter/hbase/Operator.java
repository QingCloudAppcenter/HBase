package com.qingcloud.appcenter.hbase;

import com.qingcloud.appcenter.hbase.coprocessor.ExampleRowCounter;
import com.qingcloud.appcenter.hbase.coprocessor.RowCounter;
import com.qingcloud.appcenter.hbase.util.Constant;
import com.qingcloud.appcenter.hbase.util.HBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * used for local testing
 * */

public class Operator {
    private static final Logger logger = LoggerFactory.getLogger(Operator.class);

    public static void main(String[] args) throws Exception {
        // init args
        Constant.init(args);
        HBaseUtil.init(Constant.HBaseConfPath);
        MyHbaseTable table;

        switch (Constant.Action) {
            case "create_table": // java *.jar -a create_table -t ns1:table1 -c base_info,other_info
                logger.info("create hbase table: {}.", Constant.TableName);
                table = new MyHbaseTable(Constant.TableName, Constant.ColumnFamilies);
                table.create();
                break;

            case "add_rows": // java *.jar -a add_rows -t ns1:table1 [-n 1000000 -b 500000 -s 1]
                if (!HBaseUtil.tableExist(Constant.TableName)){
                    logger.error("The table {} not exist!", Constant.TableName);
                    System.exit(1);
                }

                String start = Constant.StartRowKey == null ? Constant.DefaultStartRowKey : Constant.StartRowKey;

                logger.info("insert {} records into {}, rowkey start at {}..",
                        Constant.RowNum, Constant.TableName, start);
                table = new MyHbaseTable(Constant.TableName,
                        Constant.ColumnFamilies, start);
                table.insert(Constant.RowNum, Constant.BatchNum);
                break;

            case "count": // java *.jar -a count -t ns1:table1
                try {
                    if (!HBaseUtil.tableExist(Constant.TableName)){
                        logger.error("The table {} not exist!", Constant.TableName);
                        System.exit(1);
                    }

                    long count = RowCounter.count();
                    logger.info("Total row count: " + count);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                break;

            case "example_count": // java *.jar -a example_count -t ns1:table1
                try {
                    if (!HBaseUtil.tableExist(Constant.TableName)){
                        logger.error("The table {} not exist!", Constant.TableName);
                        System.exit(1);
                    }

                    long count = ExampleRowCounter.count(Constant.TableName);
                    logger.info("Total row count: " + count);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                break;

            default:
                logger.error("Un-support action: {}", Constant.Action);
                System.exit(1);
        }
    }
}
