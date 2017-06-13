gemfire-client

This project performs putall operation on the cluster. 

1. To make it talk to the desired cluster, update the gemfire-transfer-parent\gemfire-client\src\main\resources\client\geode-client-cache.xml with the correct locators.

2. If the client is going against a secured cluster, add the gfsecurity.properties in the classpath with the following contents

ssl-enabled=true
javax.net.ssl.keyStoreType=jks
javax.net.ssl.trustStore=<trust store path; for POC use /opt/java/jre/lib/security/cacerts>
javax.net.ssl.trustStorePassword=<trust store password; for POC use changeit>


gemfire-changes-subscriber

This Spring Boot app functions as a subscriber to messages that are published to a Kafka topic, processes them and puts them into gemfire

1. To point it to the correct Kafka and zookeeper servers, update the application.yml. The 
spring.cloud.bindings.destination is the topic name on which the subscriber will listen to. If more than one app is running on the same server, update the server.port.

2.If subscriber is putting messages to a secured gemfire cluster,

add the gfsecurity.properties in the classpath with the following contents

ssl-enabled=true
javax.net.ssl.keyStoreType=jks
javax.net.ssl.trustStore=<trust store path; for POC use /opt/java/jre/lib/security/cacerts>
javax.net.ssl.trustStorePassword=<trust store password; for POC use changeit>

3. To subscribe to a specific gemfire cluster, update the client-cache.xml in src/main/resources to connect to the correctlocators in the pool and point the client cache to that pool.

gemfire-transfer-cachelistener

This project creates a CacheListener that can be registered on GemFire regions to publish events to Kafka topics

1. To point to specific Kafka and zookeeper nodes, update gemfire-transfer-parent\gemfire-transfer-cachelistener\src\main\resources\gemfire-transfer-cachelistener.properties
Update the gemfire.cluster.name to the cluster on which the events are getting generated. The region name and this property will determine the Kafka topic to which messages will be published. eg. if the region is called customer and gemfire.cluster.name is poc-blue. The Kafka topic to which messages will be published is customer-poc-blue. Make sure the subscriber for this topic listens on this name.

2. to build this project use mvn assembly:assembly -DdescriptorId=jar-with-dependencies

3. To register this cache listener on the gemfire servers,

	a. copy the jars gemfire-transfer-cachelistener/target/gemfire-transfer-cachelistener-0.0.1-SNAPSHOT-jar-with-dependencies.jar and /gemfire-transfer-commons/target/gemfire-transfer-commons-0.0.1-SNAPSHOT.jar to the gemfire servers. 
	b. Change permissions to 755
	c. change user and group to gemfire:gemfire
	d. Copy the jars to /opt/gemire/config/lib
	e. Restart the gemfire process using /etc/init.d/gemfire restart
	f. Repeat for all servers.
