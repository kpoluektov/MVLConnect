FROM bitnami/kafka AS runtime

COPY /target/scala-2.12/MVLConnect-assembly-0.1.jar /usr/bin/MVLConnect-assembly-0.1.jar
COPY /src/main/resources/connect-standalone.properties /usr/bin/connect-standalone.properties
COPY /src/main/resources/connect-distributed.properties /usr/bin/connect-distributed.properties
COPY /src/main/resources/MVLConnector.properties /usr/bin/MVLConnector.properties

CMD connect-distributed.sh /usr/bin/connect-distributed.properties /usr/bin/MVLConnector.properties


