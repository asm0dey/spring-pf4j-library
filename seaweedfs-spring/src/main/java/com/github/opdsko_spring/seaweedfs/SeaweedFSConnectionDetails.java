package com.github.opdsko_spring.seaweedfs;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import java.net.InetSocketAddress;

public interface SeaweedFSConnectionDetails extends ConnectionDetails {
    InetSocketAddress masterAddress();
    
    InetSocketAddress filerAddress();
    InetSocketAddress filerGrpcAddress();
}