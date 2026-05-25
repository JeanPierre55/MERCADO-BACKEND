#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# This script is a wrapper around Maven that downloads and runs Maven if necessary.

set -e

PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")"/$link"
  fi
done

MAVEN_HOME=$(dirname "$PRG")
MAVEN_HOME=$(cd "$MAVEN_HOME" && pwd)

MAVEN_WRAPPER_JAR="$MAVEN_HOME/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_HOME/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  echo "Downloading Maven wrapper..."
  mkdir -p "$MAVEN_HOME/.mvn/wrapper"
  curl -fsSL -o "$MAVEN_WRAPPER_JAR" "$(grep distributionUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2 | tr -d ' ' | sed 's/.*\///' | sed 's/-bin.zip//' | xargs -I {} echo 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar')"
fi

exec java -cp "$MAVEN_WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
