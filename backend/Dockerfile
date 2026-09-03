# ---------- Etapa 1: compilar ----------
# Usa uma imagem que já tem Maven e JDK 21 instalados.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia só o pom primeiro e baixa as dependências.
# Assim, se o código mudar mas o pom não, o Docker reaproveita esta camada
# e o build seguinte fica muito mais rápido.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: executar ----------
# Imagem só com o JRE: não precisa de Maven nem do código-fonte para rodar.
# Resultado bem menor e com menos superfície de ataque.
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# A porta real vem da variável PORT injetada pelo provedor (application.yml).
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
