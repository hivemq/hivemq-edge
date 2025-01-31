# How to create Docker Images via the OCI plugin

Context: We use an OCI plugin to create images for HiveMQ Edge instead of the DockerFile + build script approach. 

```
./gradlew loadOciImage
```

Docker will load the image of HiveMQ Edge automatically into its image store
