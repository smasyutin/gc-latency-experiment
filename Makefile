RESULTS=go/results.txt java/results.txt java/results-graalvm.txt java/results-graalvm-native.txt java/results-jdk8.txt java/results-jdk15.txt kotlin/results.txt kotlin/results-native.txt kotlin/results-jdk15.txt node/results.txt
RUN_PARAMS=10000000 200000

.PHONY: all build clean

all: $(RESULTS)

clean:
	rm -f $(RESULTS)

build:
	docker build -t gc-go -f go/Dockerfile go
	docker build -t gc-java -f java/Dockerfile java
	docker build -t gc-java-graalvm -f java/Dockerfile.graalvm java
	docker build -t gc-java-graalvm-native -f java/Dockerfile.graalvm-native java
	docker build -t gc-java-jdk8 -f java/Dockerfile.jdk8 java
	docker build -t gc-java-jdk15 -f java/Dockerfile.jdk15 java
	docker build -t gc-kotlin -f kotlin/Dockerfile kotlin
	docker build -t gc-kotlin-native -f kotlin/Dockerfile.native kotlin
	docker build -t gc-kotlin-jdk15 -f kotlin/Dockerfile.jdk15 kotlin
	docker build -t gc-node -f node/Dockerfile node

go/results.txt: go/Dockerfile go/main.go
	docker run gc-go $(RUN_PARAMS) > $@

java/results.txt: java/Dockerfile java/Main.java
	docker run gc-java $(RUN_PARAMS) > $@

java/results-graalvm.txt: java/Dockerfile.graalvm java/Main.java
	docker run gc-java-graalvm $(RUN_PARAMS) > $@

java/results-graalvm-native.txt: java/Dockerfile.graalvm-native java/Main.java
	docker run gc-java-graalvm-native $(RUN_PARAMS) > $@

java/results-jdk8.txt: java/Dockerfile.jdk8 java/Main.java
	docker run gc-java-jdk8 $(RUN_PARAMS) > $@

java/results-jdk15.txt: java/Dockerfile.jdk15 java/Main.java
	docker run gc-java-jdk15 $(RUN_PARAMS) > $@

kotlin/results.txt: kotlin/Dockerfile kotlin/main.kt
	docker run gc-kotlin $(RUN_PARAMS) > $@

kotlin/results-native.txt: kotlin/Dockerfile.native kotlin/main.kt
	docker run gc-kotlin-native $(RUN_PARAMS) > $@

kotlin/results-jdk15.txt: kotlin/Dockerfile.jdk15 kotlin/main.kt
	docker run gc-kotlin-jdk15 $(RUN_PARAMS) > $@

node/results.txt: node/Dockerfile node/main.js
	docker run gc-node $(RUN_PARAMS) > $@
