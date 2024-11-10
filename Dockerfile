FROM openjdk:11-jre-slim

# Install dependencies
RUN apt-get update && apt-get install -y curl

# Download and install HiveMQ Edge
RUN curl -L https://www.hivemq.com/downloads/hivemq-edge-4.4.0.tar.gz -o hivemq-edge.tar.gz
RUN tar -xzvf hivemq-edge.tar.gz
RUN rm hivemq-edge.tar.gz

# Set working directory
WORKDIR /hivemq-edge

# Expose the port used by MQTT (default: 1883)
EXPOSE 1883

# Start HiveMQ Edge
CMD ["./bin/hivemq-edge", "--bind-address", "0.0.0.0"]
