# Etapa 1: compila. Copiamos o pom.xml sozinho primeiro para que o download
# das dependencias fique em cache e nao repita a cada mudanca de codigo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Etapa 2: so o necessario para rodar. A imagem final leva apenas o JRE e o
# jar, ficando bem menor que a de compilacao.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# A plataforma define a porta pela variavel PORT; o application.yml ja a le.
EXPOSE 8080

# MaxRAMPercentage faz a JVM respeitar o limite do container em vez de olhar
# a memoria da maquina inteira, que e o motivo classico de um container Java
# ser morto por falta de memoria.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -jar app.jar"]
