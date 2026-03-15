# Build stage
FROM maven:3-openjdk-22 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:22-jdk-slim
WORKDIR /app

# Sử dụng *.war để lấy file build ra từ thư mục target và đổi tên thành oneshop.war
COPY --from=build /app/target/*.war oneshop.war
EXPOSE 8080 

ENTRYPOINT ["java", "-jar", "oneshop.war"]