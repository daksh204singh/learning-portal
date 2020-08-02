package com.daksh.ibm.intenship.learningportal.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.Exceptions;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Utility class containing helper methods
 */
@Slf4j
public class Utility {
    /**
     * Concatenates buffers in the list into a ByteBuffer
     * @param buffers
     * @return
     */
    public static ByteBuffer concatBuffers(List<DataBuffer> buffers) {
        log.info("[I198] creating ByteBuffer from {} chunks", buffers.size());

        int partSize = 0;
        for (DataBuffer buffer: buffers)
            partSize += buffer.readableByteCount();

        ByteBuffer partData = ByteBuffer.allocate(partSize);
        buffers.forEach(dataBuffer -> partData.put(dataBuffer.asByteBuffer()));

        // Reset read pointer to first byte
        partData.rewind();

        log.info("[I208] partData: size{}", partData.capacity());

        return partData;
    }

}
