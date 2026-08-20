FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package
RUN PLAYWRIGHT_BROWSERS_PATH=/ms-playwright mvn -B -q exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium"

FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Tokyo \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    JAVA_OPTS=""

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        fontconfig \
        fonts-liberation \
        fonts-noto-cjk \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcairo2 \
        libcups2 \
        libdbus-1-3 \
        libdrm2 \
        libgbm1 \
        libglib2.0-0 \
        libgtk-3-0 \
        libnspr4 \
        libnss3 \
        libpango-1.0-0 \
        libu2f-udev \
        libvulkan1 \
        libx11-6 \
        libxcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxkbcommon0 \
        libxrandr2 \
        libxshmfence1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system botjava \
    && useradd --system --gid botjava --home-dir /app --shell /usr/sbin/nologin botjava \
    && mkdir -p /app/logs /app/tmp \
    && chown -R botjava:botjava /app

COPY --from=build /workspace/target/botjava-0.0.1.jar /app/botjava.jar
COPY --from=build /ms-playwright /ms-playwright

RUN chown -R botjava:botjava /ms-playwright /app

USER botjava

EXPOSE 8081 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/botjava.jar"]
