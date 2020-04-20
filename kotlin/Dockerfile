FROM smasyutin/kotlin:1.3.72

COPY main.kt .
RUN kotlinc -jvm-target 12 main.kt

ENV JAVA_OPS="-Xmx1G -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
ENTRYPOINT ["kotlin", "MainKt"]
CMD ["1000000", "200000"]
