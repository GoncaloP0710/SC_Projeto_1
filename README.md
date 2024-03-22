Authors:

André Reis fc58192
Gonçalo Pinto fc58178
José Brás fc55449

Compilar:

javac server/*.java client/*.java
jar cfe .\IoTServer.jar server.IoTServer server/*.class
jar cfe ./IoTDevice.jar client.IoTDevice .\client/*.class

Executar:

Server:
java -jar IoTServer.jar 
Client:
java -jar IoTDevice.jar

Limitações do trabalho:

O trabalho não apresenta limitações