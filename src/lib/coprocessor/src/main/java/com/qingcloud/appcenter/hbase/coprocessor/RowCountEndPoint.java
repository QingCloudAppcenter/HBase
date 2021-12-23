package com.qingcloud.appcenter.hbase.coprocessor;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessor;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.regionserver.RegionScanner;
import org.apache.hadoop.hbase.shaded.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * used for region-server coprocessor
 *
 * @author appcenter
 * */

public class RowCountEndPoint extends PbCoprocessor.CountService implements RegionCoprocessor {

    private static final Logger logger = LoggerFactory.getLogger(RowCountEndPoint.class);

    private RegionCoprocessorEnvironment regionCoprocessorEnv = null;
    private Boolean running = true;

    private long count(PbCoprocessor.CountRowRequest request) throws IOException {
        if (this.regionCoprocessorEnv == null) {
            logger.error("===========RegionCoprocessorEnv is null");
            return 0;
        }
        long count = 0;

        Scan scan = new Scan();
        scan.setFilter(new FirstKeyOnlyFilter());

        if (request.getScanBatch() > 0) {
            scan.setBatch(request.getScanBatch());
        }

        if (!request.getStartRowKey().isEmpty()) {
            scan.withStartRow(Bytes.toBytes(request.getStartRowKey()));
        }
        if (!request.getEndRowKey().isEmpty()) {
            scan.withStopRow(Bytes.toBytes(request.getEndRowKey()));
        }

        try (RegionScanner scanner = this.regionCoprocessorEnv.getRegion().getScanner(scan)) {
            List<Cell> cells = new ArrayList<Cell>();
            boolean hasMore;
            do {
                hasMore = scanner.nextRaw(cells);
                if (cells.size() > 0) {
                    count ++;
                }
                cells.clear();
            } while (hasMore && this.running);
        } catch (Exception e) {
            if (!this.running) {
                logger.error("Count row process was stopped!");
            } else {
                logger.error("count row failure on region server!");
            }
            throw new RuntimeException(e);
        }
        return count;
    }

    @Override
    public void countRow(RpcController controller, PbCoprocessor.CountRowRequest request,
                         RpcCallback<PbCoprocessor.CountRowResponse> done) {
        PbCoprocessor.CountRowResponse response = null;
        logger.info("Start to count row of table..");
        try {
            long count = this.count(request);
            response = PbCoprocessor.CountRowResponse.newBuilder().setCount(count).build();
        } catch (IOException e) {
            ResponseConverter.setControllerException(controller, e);
        }
        done.run(response);
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        this.regionCoprocessorEnv = (RegionCoprocessorEnvironment) env;
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        // do Nothing
        logger.warn("stop row count coprocessor.");
        this.running = false;
    }

    @Override
    public Iterable<Service> getServices() {
        return Collections.singleton(this);
    }
}
