FROM openjdk:17-alpine as builder

WORKDIR saejinaapp
COPY . .

RUN ./mvnw install -DskipTests

FROM openjdk:17-alpine

RUN ["adduser", "--disabled-password", "saejina"]
USER saejina
WORKDIR saejinaapp

COPY --from=builder /saejinaapp/target/saejina-app.jar .

EXPOSE 8080

CMD ["java", "-jar", "/saejinaapp/saejina-app.jar"]