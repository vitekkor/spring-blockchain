FROM gradle:7.6.0-jdk11-alpine as builder
WORKDIR /vitekkor/vitcoin
COPY . .
RUN gradle clean bootJar

FROM openjdk:11-jre-slim
WORKDIR /vitekkor/vitcoin
COPY --from=builder /vitekkor/vitcoin/blockchain/build/libs/blockchain-1.0-SNAPSHOT.jar .
CMD java -jar blockchain-1.0-SNAPSHOT.jar --spring.config.location=classpath:/application.yml,optional:/etc/vitekkor/vitcoin/application.yml
