Authors:
Grupo: SegC-014
André Reis fc58192
Gonçalo Pinto fc58178
José Brás fc55449

Compilar:

javac server/*.java client/*.java
jar cfe IoTServer.jar server.IoTServer server/*.class
jar cfe IoTDevice.jar client.IoTDevice client/*.class

Executar:

Server:
java -jar IoTServer.jar 
Client:
java -jar IoTDevice.jar

Limitações do trabalho:

Após compilar, o tamanho do ficheiro IoTDevice.jar pode ser diferente do especificado para o server em testFile.csv dependendo da máquina onde é compilado.
O tamanho escrito "5719" é o tamanho jar quando compilado nas máquinas do laboratório da FCUL