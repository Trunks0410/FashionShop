FROM eclipse-temurin:22-jdk-jammy

WORKDIR /app

COPY target/*.jar oneshop.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","oneshop.jar"]