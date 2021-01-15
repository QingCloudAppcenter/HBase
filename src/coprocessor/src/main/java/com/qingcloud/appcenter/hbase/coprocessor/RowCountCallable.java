package com.qingcloud.appcenter.hbase.coprocessor;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcUtils.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;

import java.io.IOException;

public class RowCountCallable implements Batch.Call<PbCoprocessor.CountService, Long> {

    private PbCoprocessor.CountRowRequest request;

    public RowCountCallable(PbCoprocessor.CountRowRequest request) {
        this.request = request;
    }

    @Override
    public Long call(PbCoprocessor.CountService instance) throws IOException {

        BlockingRpcCallback<PbCoprocessor.CountRowResponse> rpcCallback =
                new BlockingRpcCallback<PbCoprocessor.CountRowResponse>();
        ServerRpcController controller = new ServerRpcController();

        instance.countRow(controller, this.request, rpcCallback);
        final PbCoprocessor.CountRowResponse response = rpcCallback.get();

        if (controller.failedOnException()) {
            throw controller.getFailedOn();
        }
        return (response != null && response.hasCount()) ? response.getCount() : 0;
    }

}
