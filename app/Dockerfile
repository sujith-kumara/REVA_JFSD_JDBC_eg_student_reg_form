# Use OpenJDK 17 base image
FROM openjdk:17

# Set working directory
WORKDIR /usr/src/app

# Copy MySQL JDBC driver
COPY lib/mysql-connector-j-9.5.0.jar .

# Copy Java source and web folder
COPY Server.java .
COPY web ./web

# Compile Java code with JDBC driver
RUN javac -cp .:mysql-connector-j-9.5.0.jar Server.java

# Run the server with JDBC driver in classpath
CMD ["java", "-cp", ".:mysql-connector-j-9.5.0.jar", "Server"]
