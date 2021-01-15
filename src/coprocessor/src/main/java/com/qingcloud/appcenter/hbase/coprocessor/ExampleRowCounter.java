package com.qingcloud.appcenter.hbase.coprocessor;

import com.qingcloud.appcenter.hbase.util.HBaseUtil;
import com.qingcloud.appcenter.hbase.util.Constant;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.coprocessor.example.generated.ExampleProtos;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcUtils.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

// // load coprocessor example
// alter 'ns1:table1', METHOD => 'table_att', 'coprocessor'=>'|org.apache.hadoop.hbase.coprocessor.example.RowCountEndpoint ||'

public class ExampleRowCounter {

    private static final Logger logger = LoggerFactory.getLogger(ExampleRowCounter.class);

    public static long count(String tableName) throws Throwable {
        String start = Constant.DateFormatCons.format(new Date());
        Table table = HBaseUtil.getTable(tableName);
        final ExampleProtos.CountRequest request = ExampleProtos.CountRequest.getDefaultInstance();

        Map<byte[], Long> results = table.coprocessorService(ExampleProtos.RowCountService.class,
                null, null,
                new Batch.Call<ExampleProtos.RowCountService, Long>() {
                    public Long call(ExampleProtos.RowCountService counter) throws IOException {
                        ServerRpcController controller = new ServerRpcController();
                        BlockingRpcCallback<ExampleProtos.CountResponse> rpcCallback =
                                new BlockingRpcCallback<ExampleProtos.CountResponse>();

                        //实现在server端
                        counter.getRowCount(controller, request, rpcCallback);
                        ExampleProtos.CountResponse response = rpcCallback.get();
                        if (controller.failedOnException()) {
                            throw controller.getFailedOn();
                        }
                        return (response != null && response.hasCount()) ? response.getCount() : 0;
                    }
                });

        long sum = 0;
        long regionCount = 0;

        logger.info("#########################################");
        for (Map.Entry<byte[], Long> result : results.entrySet()) {
            logger.info("Region: {}, RowCount: {};", Bytes.toString(result.getKey()), result.getValue());
            sum += result.getValue();
            regionCount++;
        }

        String end = Constant.DateFormatCons.format(new Date());
        logger.info("Start count at: {}", start);
        logger.info("Done count at: {}", end);
        logger.info("Region Counts = {}", regionCount);
        logger.info("Total Row Counts = {}", sum);
        logger.info("#########################################");
        return sum;
    }
}
