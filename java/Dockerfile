FROM adoptopenjdk:14-hotspot

COPY Main.java .
RUN javac Main.java

ENV JAVA_OPS="-Xmx512M -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
ENTRYPOINT ["java", "Main"]
CMD ["1000000", "200000"]
