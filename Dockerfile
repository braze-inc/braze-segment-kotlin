FROM bellsoft/liberica-openjdk-debian:17.0.13

# Dependencies for Android SDK and Gradle
RUN apt-get update && apt-get install --no-install-recommends \
  wget curl git zip unzip ca-certificates -y

RUN update-ca-certificates

ENV ANDROID_HOME=/opt/android-sdk-linux \
  GRADLE_USER_HOME=/opt/gradle-user-home

# Android SDK command-line tools
RUN cd /opt \
  && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O android-commandline-tools.zip \
  && mkdir -p ${ANDROID_HOME}/cmdline-tools \
  && unzip -q android-commandline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
  && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
  && rm android-commandline-tools.zip

ENV PATH=${PATH}:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/cmdline-tools/latest/bin

# Android SDK components
RUN yes | sdkmanager --licenses >/dev/null \
  && touch /root/.android/repositories.cfg

RUN sdkmanager "tools" "platform-tools" \
  && yes | sdkmanager --update --channel=3 | grep -v = || true \
  && yes | sdkmanager \
  "build-tools;36.0.0" \
  "platforms;android-36" \
  | grep -v = || true

# Pre-download Gradle (WORKDIR /app is used by docker-compose mount)
WORKDIR /app
COPY gradlew .
COPY gradle gradle
RUN ./gradlew -v
