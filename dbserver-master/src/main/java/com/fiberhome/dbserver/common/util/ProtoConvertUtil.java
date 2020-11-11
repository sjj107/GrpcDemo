package com.fiberhome.dbserver.common.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.fiberhome.dbserver.common.elements.server.ServerName;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.InputFormatProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.OutputFormatProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.PartitionDescriptorProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.PartitionLoadTypeProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.PartitionStatusProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.PartitionTypeProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.RegionInfoProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.RegionLocationProto;
import com.fiberhome.dbserver.common.transport.protobuf.DBServerProtos.RegionTypeProto;
import com.fiberhome.dbserver.common.transport.protobuf.SlaveServerProtos;
import com.fiberhome.dbserver.common.transport.protobuf.SlaveServerProtos.RegionCreateRequest;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

/**
 * Proto转换
 *
 * @author lizhen, xiajunsheng 2020/10/20
 * @since 1.0.0
 */
public class ProtoConvertUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(ProtoConvertUtil.class);

    private ProtoConvertUtil()
    {
    }

    /**
     * <p>
     * 将server端的ServerName对象转化成proto中的ServerNameProto对象
     * </p>
     *
     * @param serverName server端的ServerName对象
     * @return proto中的ServerNameProto对象
     */
    public static DBServerProtos.ServerNameProto toServerNameProto(ServerName serverName)
    {
        DBServerProtos.ServerNameProto.Builder serverNameBuider = DBServerProtos.ServerNameProto.newBuilder();
        serverNameBuider.setIp(serverName.getHostIp());
        serverNameBuider.setHostName(serverName.getHostIp());
        serverNameBuider.setPort(serverName.getPort());
        serverNameBuider.setStartTime(serverName.getStartTime());
        return serverNameBuider.build();
    }





}
