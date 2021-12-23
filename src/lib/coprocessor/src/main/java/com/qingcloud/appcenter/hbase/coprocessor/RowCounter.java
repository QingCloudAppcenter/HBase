package com.qingcloud.appcenter.hbase.coprocessor;

import com.qingcloud.appcenter.hbase.util.HBaseUtil;
import com.qingcloud.appcenter.hbase.util.Constant;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
* used to count rows
* */

// load coprocessor
// alter 'ns1:table1','Coprocessor'=>'hdfs://cln-p2hnwwtr:9000/hbase_jar/hbase-coprocessor-rowcounter-1.0-SNAPSHOT.jar|com.qingcloud.appcenter.hbase.coprocessor.RowCountEndPoint||'
// remove coprocessor
// alter 'ns1:table1',METHOD => 'table_att_unset', NAME=> 'coprocessor$1'

public class RowCounter {

    private static final Logger logger = LoggerFactory.getLogger(RowCounter.class);

    public static long count() throws Throwable {
        String start = Constant.DateFormatCons.format(new Date());
        try {
            Table table = HBaseUtil.getTable(Constant.TableName);
            PbCoprocessor.CountRowRequest.Builder builder = PbCoprocessor.CountRowRequest.newBuilder();
            PbCoprocessor.CountRowRequest request;

            // set scanBatch
            if (Constant.ScanBatchNum > 0) {
                if (Constant.BatchNum > Constant.MaxScanBatchNum) {
                    throw new Exception("The scan batch must less than: " + Constant.MaxScanBatchNum);
                }
                builder.setScanBatch((int) Constant.BatchNum);
            }

            // set startRowKey endRowKey
            if (!Constant.isEmpty(Constant.StartRowKey)) {
                builder.setStartRowKey(Constant.StartRowKey);
            }
            if (!Constant.isEmpty(Constant.EndRowKey)) {
                builder.setEndRowKey(Constant.EndRowKey);
            }
            request = builder.build();

            final Map<byte[], Long> results = table.coprocessorService(
                    PbCoprocessor.CountService.class, null, null, new RowCountCallable(request));

            long totalRows = 0;
            logger.info("#########################################");
            for (Map.Entry<byte[], Long> entry : results.entrySet()) {
                totalRows += entry.getValue();
                logger.info("Region: {}, RowCount: {};", Bytes.toString(entry.getKey()),
                        Constant.NF.format(entry.getValue()));
            }

            String end = Constant.DateFormatCons.format(new Date());
            logger.info("Start counting at: {}", start);
            logger.info("Count done at: {}", end);
            logger.info("Total rows: {}", Constant.NF.format(totalRows));
            logger.info("#########################################");
            return totalRows;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        // init args
        Constant.init(args);
        if (Constant.Help) {
            System.out.println("Usage: ");
            System.out.println("  java -jar hbase-coprocessor-rowcounter-1.0-SNAPSHOT.jar -t table_name [-b ScanBatchNum] [-s startRowKey] [-e stopRowKey] [-f /opt/hbase/conf/hbase-site.xml] ");
            System.exit(0);
        }
        HBaseUtil.init(Constant.HBaseConfPath);

        try {
            if (!HBaseUtil.tableExist(Constant.TableName)){
                logger.error("The table {} not exist!", Constant.TableName);
                System.exit(1);
            }
            count();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
