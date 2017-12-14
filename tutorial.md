## Grpc server / client demo
##### 1. Add `scalapb` plugin
In `project/plugins.sbt` add
```
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")
```

##### 2. Configure `scalabp` in `build.sbt`
`PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)`

##### 3. Add `scalapb-grpc` dependencies in `build.sbt`
```
libraryDependencies ++= Seq(
 "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
 "io.grpc" % "grpc-netty" % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
)
```

##### 4. Create `helloworld.proto` protobuf file in `src/main/protobuf` directory
```
// protocol version 3
syntax = "proto3";

// package
package com.example;

// -----------------
// protobuf messages
// -----------------

// request message
message HelloWorldRequest {
    string name = 1;
}

// response message
message HelloWorldResponse {
    string message = 1;
}

// -------------
// grpc-services
// -------------
service HelloWorld {
    rpc sayHello(HelloWorldRequest) returns (HelloWorldResponse);
}
```

##### 5. Compile project
This automatically generates protobuf messages and grpc stubs in `target` directory
```sh
$ sbt "clean compile"
```

##### 6. Create sources packages
1. Create package `com.example` in `src/main/scala` directory

##### 7. Create grpc-server and HelloWorldService implementation
1. Create file `GrpcServer.scala`

```scala
package com.example

import com.example.helloworld.{ HelloWorldGrpc, HelloWorldRequest, HelloWorldResponse }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ Server, ServerBuilder, ServerServiceDefinition }

import scala.concurrent.{ ExecutionContext, Future }

object GrpcServer extends App with LazyLogging {
  val executionContext = ExecutionContext.global
  val helloService: ServerServiceDefinition =
    HelloWorldGrpc.bindService(new HelloWorldImpl()(executionContext), executionContext)
  val serverPort: Int = 9000
  val server: Server = ServerBuilder
    .forPort(serverPort)
    .addService(helloService)
    .build()

  // start server
  server.start()
  logger.info(s"grpc server started in port $serverPort")

  server.awaitTermination()
}

class HelloWorldImpl()(implicit ec: ExecutionContext) extends HelloWorldGrpc.HelloWorld {
  override def sayHello(request: HelloWorldRequest): Future[HelloWorldResponse] = {
    Future(HelloWorldResponse(s"Hello to you ${request.name}"))
  }
}
```

##### 8. Create grpc-client
1. Create file `GrpcClient.scala` in `src/main/scala/com/example` directory

```scala
package com.example

import com.example.helloworld.{ HelloWorldGrpc, HelloWorldRequest, HelloWorldResponse }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ ManagedChannel, ManagedChannelBuilder }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object GrpcClient extends App with LazyLogging {
  val serverAddress: String = "localhost"
  val serverPort: Int       = 9000
  val serverChannel: ManagedChannel = ManagedChannelBuilder
    .forAddress(serverAddress, serverPort)
    .usePlaintext(true)
    .build()

  // client instance
  val helloWorldClient: HelloWorldClient = new HelloWorldClient(serverChannel)

  // blocking call
  val blockingCall: HelloWorldResponse = helloWorldClient.sayHelloBlocking("BLOCKING")
  logger.info(s"BLOCKING RESPONSE $blockingCall")

  // async call

  // this is going to be executed in another thread
  val asyncCall: Future[HelloWorldResponse] = helloWorldClient.sayHelloAsync("ASYNC")
  // we should never do this
  // we're going to await for async response ONLY for demo purpose
  val asyncResponse: HelloWorldResponse = Await.result(asyncCall, 10.seconds)
  logger.info(s"ASYNC RESPONSE $asyncResponse")
}

class HelloWorldClient(serverChannel: ManagedChannel) extends LazyLogging {

  private val asyncStub    = HelloWorldGrpc.stub(serverChannel)
  private val blockingStub = HelloWorldGrpc.blockingStub(serverChannel)

  def sayHelloAsync(name: String): Future[HelloWorldResponse] = {
    logger.info("calling say hello (async)")
    val request = HelloWorldRequest(name)
    asyncStub.sayHello(request)
  }

  def sayHelloBlocking(name: String): HelloWorldResponse = {
    logger.info("calling say hello (blocking)")
    val request = HelloWorldRequest(name)
    blockingStub.sayHello(request)
  }
}
```

##### 9. Run grpc server / client
1. Run this commands in different terminals
```sh
$ sbt ";runMain com.example.GrpcServer" 
$ sbt ";runMain com.example.GrpcClient" 
```