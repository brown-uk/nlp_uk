FROM ubuntu:24.04

RUN apt update
RUN apt install -y openjdk-17-jdk-headless unzip
ADD . /opt/nlp_uk
# ADD ./gradle-8.14.3-bin.tgz /opt/
ADD https://services.gradle.org/distributions/gradle-8.14.3-bin.zip /opt/gradle/
RUN unzip -q /opt/gradle/gradle-8.14.3-bin.zip -d /opt/gradle && \
    ln -s /opt/gradle/gradle-8.14.3 /opt/gradle/latest
WORKDIR /opt/nlp_uk
RUN rm -f /opt/nlp_uk/src/main/resources/ua/net/nlp/tools/stats/*
#RUN /opt/gradle-8.14.3/bin/gradle --no-daemon --offline -PlocalLib compileGroovy
RUN /opt/gradle/latest/bin/gradle --no-daemon --offline tagText -PlocalLib -Pargs="-g README.md"
