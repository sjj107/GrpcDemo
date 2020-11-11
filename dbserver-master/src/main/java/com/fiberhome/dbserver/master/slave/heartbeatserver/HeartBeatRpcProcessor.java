package com.fiberhome.dbserver.master.slave.heartbeatserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.transport.protobuf.HeartBeatMangerServiceGrpc;
import com.fiberhome.dbserver.common.transport.protobuf.MasterServerProtos;
import io.grpc.stub.StreamObserver;

/**
 * 心跳处理器
 *
 * @author lizhen, 2020/10/22
 * @since 1.0.0
 */
public class HeartBeatRpcProcessor extends HeartBeatMangerServiceGrpc.HeartBeatMangerServiceImplBase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatRpcProcessor.class);

    private HeartBeatManager heartBeatManager;

    HeartBeatRpcProcessor(HeartBeatManager heartBeatManager)
    {
        this.heartBeatManager = heartBeatManager;
    }

    /**
     * <pre>
     * 心跳上报
     * </pre>
     *
     * @param request 心跳信息
     * @param responseObserver 心跳响应
     */
    @Override
    public void reportHeartBeat(MasterServerProtos.HeartBeatRequest request,
                                StreamObserver<MasterServerProtos.HeartBeatResponse> responseObserver)
    {
        LOGGER.info("receive heartInfo: {}", request);
        //获取ServerName
        
        //获取ServerLoad
        
        //SlaveManger 处理心跳信息
        responseObserver.onNext(MasterServerProtos.HeartBeatResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
