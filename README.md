# Portable CRaC: Jetty example application

Modified Jetty example to demonstrate the portable mode of CRaC.

## How to run

1. Compile:
   ```shell
   mvn package
   ```
2. Checkpoint:
   ```shell
   # (shell 1) Launch
   $JAVA_HOME/bin/java -XX:CREngine="" -XX:CRaCCheckpointTo=cr \
     -Djdk.crac.resource-policies=res-policies.yaml \
     -jar target/example-jetty-1.0-SNAPSHOT.jar
   # (shell 2) Warm up
   curl localhost:8080
   # (shell 2) Checkpoint
   curl localhost:8080/checkpoint
   # (shell 1) Use Ctrl-C to stop the app
   ```
3. Restore:
   ```shell
   # (shell 1) Restore
   $JAVA_HOME/bin/java -XX:CREngine="" -XX:CRaCRestoreFrom=cr \
     -Djdk.crac.resource-policies=res-policies.yaml \
     -jar target/example-jetty-1.0-SNAPSHOT.jar
   # (shell 2) Use the restored server
   curl localhost:8080
   ```

## Additional information

For more information check:
- [What is CRaC?](https://docs.azul.com/core/crac/crac-introduction)
- About the original example application: [Step-by-step CRaC support for a Jetty app](https://github.com/CRaC/docs/blob/master/STEP-BY-STEP.md).
