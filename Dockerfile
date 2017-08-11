FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD dist/skyprofiler-1.0-RELEASE.jar app.jar

ENV ZOOKEEPER_HOST localhost
ENV MONGODB_HOST localhost

EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS \
  -Dkafka.zookeeper.connect=$ZOOKEEPER_HOST -Dspring.data.mongodb.host=$MONGODB_HOST \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app.jar" ]