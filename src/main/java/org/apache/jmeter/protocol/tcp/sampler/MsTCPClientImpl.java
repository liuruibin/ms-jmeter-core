/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.protocol.tcp.sampler;

import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sample TCPClient implementation.
 * Reads data until the defined EOL byte is reached.
 * If there is no EOL byte defined, then reads until
 * the end of the stream is reached.
 * The EOL byte is defined by the property "tcp.eolByte".
 */
public abstract class MsTCPClientImpl extends TCPSampler {
    private static final Logger log = LoggerFactory.getLogger(MsTCPClientImpl.class);

    private static final int EOL_INT = JMeterUtils.getPropDefault("tcp.eolByte", 1000); // $NON-NLS-1$

    public void write(OutputStream os, String s , String charset)  throws IOException{
        if(log.isDebugEnabled()) {
            log.debug("WriteS: {}", showEOL(s));
        }
        os.write(s.getBytes(charset));
        os.flush();
    }

    public void write(OutputStream os, InputStream is , String charset) throws IOException{
        byte[] buff = new byte[512];
        while(is.read(buff) > 0){
            if(log.isDebugEnabled()) {
                log.debug("WriteIS: {}", showEOL(new String(buff, charset)));
            }
            os.write(buff);
            os.flush();
        }
    }


    private String showEOL(final String input) {
        StringBuilder sb = new StringBuilder(input.length()*2);
        for(int i=0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch < ' ') {
                sb.append('[');
                sb.append((int)ch);
                sb.append(']');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
