# 后端服务通用镜像(多阶段构建)
# 用法示例:docker build -f deploy/docker/Dockerfile --build-arg SERVICE=exam-auth -t examflow/exam-auth:1.0.0 .
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /build
# 先拷贝依赖声明以复用层缓存
COPY backend/pom.xml .
COPY backend/exam-common/pom.xml backend/exam-common/pom.xml
COPY backend/exam-gateway/pom.xml backend/exam-gateway/pom.xml
COPY backend/${SERVICE}/pom.xml backend/${SERVICE}/pom.xml
RUN mvn -q -pl backend/exam-common,backend/${SERVICE} -am dependency:go-offline -DskipTests || true

COPY backend/ backend/
RUN mvn -q -pl backend/${SERVICE} -am package -DskipTests

FROM eclipse-temurin:17-jre
ARG SERVICE
ENV SERVICE_NAME=${SERVICE}
WORKDIR /app
COPY --from=build /build/backend/${SERVICE}/target/*.jar app.jar
# 生产环境密钥注入(TDD §7.2)
ENV EXAMSECRET_KEY=""
EXPOSE 8080
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
