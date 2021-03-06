/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.examples.transport.tcp.stream;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public final class ClientStreamingToServer {

  private static final Logger logger = LoggerFactory.getLogger(ClientStreamingToServer.class);

  public static void main(String[] args) throws InterruptedException {
    RSocketServer.create(
            SocketAcceptor.forRequestStream(
                payload ->
                    Flux.interval(Duration.ofMillis(100))
                        .map(aLong -> DefaultPayload.create("Interval: " + aLong))))
        .bindNow(TcpServerTransport.create("localhost", 7000));

    RSocket socket =
        RSocketConnector.create()
            .setupPayload(DefaultPayload.create("test", "test"))
            .connect(TcpClientTransport.create("localhost", 7000))
            .block();

    final Payload payload = DefaultPayload.create("Hello");
    socket
        .requestStream(payload)
        .map(Payload::getDataUtf8)
        .doOnNext(logger::debug)
        .take(10)
        .then()
        .doFinally(signalType -> socket.dispose())
        .then()
        .block();

    Thread.sleep(1000000);
  }
}
