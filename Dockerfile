FROM gradle:7.6.0-jdk17-alpine as builder
WORKDIR /vitekkor/vitcoin
COPY . .
RUN gradle clean bootJar

FROM openjdk:17-jdk-slim
WORKDIR /vitekkor/vitcoin
COPY --from=builder /vitekkor/vitcoin/blockchain/build/libs/blockchain-1.0.0-SNAPSHOT.jar .
CMD java -jar blockchain-1.0.0-SNAPSHOT.jar --spring.config.location=classpath:/application.yml,optional:/etc/vitekkor/vitcoin/application.yml
