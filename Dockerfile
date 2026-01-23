FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

RUN apk add --no-cache dos2unix

COPY gradlew .
COPY gradle gradle

RUN dos2unix gradlew
RUN chmod +x gradlew

COPY build.gradle.kts .
COPY settings.gradle.kts .

COPY src src

RUN ./gradlew build -x test --no-daemon

RUN rm -f build/libs/*-plain.jar
RUN cp build/libs/*.jar app.jar

RUN mkdir -p /app/data

ENTRYPOINT ["java", "-jar", "app.jar"]
