
Arquitectura del sistema
El sistema usa una arquitectura serverless en AWS con un punto de entrada HTTP unico en API Gateway y dos funciones Lambda en Java 17:
·	GET /productos invoca ProductosFunction, que consulta ProductosTable (DynamoDB) y retorna el catalogo.
·	POST /ventas invoca VentasFunction, que valida el body, verifica productos en ProductosTable y registra la venta en VentasTable (DynamoDB).
Infraestructura y despliegue estan definidos como codigo con AWS SAM (template.yaml), incluyendo API, Lambdas, tablas e IAM.
Cliente
  -> API Gateway (REST, stage prod)
      -> GET /productos -> ProductosFunction -> ProductosTable
      -> POST /ventas   -> VentasFunction
                            -> ProductosTable (validacion)
                            -> VentasTable    (persistencia)
2) URL base del API Gateway desplegado
URL base:
https://bvqj72xsqk.execute-api.us-east-1.amazonaws.com/prod
Endpoints:
·	GET https://bvqj72xsqk.execute-api.us-east-1.amazonaws.com/prod/productos
·	POST https://bvqj72xsqk.execute-api.us-east-1.amazonaws.com/prod/ventas
3) Proceso SDD (Spec-Driven Development)
La implementacion se guio por SDD: primero se definieron las especificaciones y despues se escribio codigo.
Orden aplicado en el proyecto:
1.	requirements.md: define el comportamiento esperado (contratos HTTP, validaciones y errores).
2.	design.md: define arquitectura, componentes, flujos, modelos y estrategia de pruebas.
3.	tasks.md: descompone el trabajo en tareas trazables a requisitos.
4.	Implementacion: handlers, modelos, pruebas e infraestructura SAM siguiendo esas especificaciones.
Resultado: mayor trazabilidad entre requisito-diseno-codigo, menor ambiguedad en la implementacion y pruebas enfocadas en escenarios definidos desde el spec.

Para conectar AWS con Kiro, primero Kiro necesitaba credenciales de AWS. Como no tenía ninguna configurada, creé un usuario IAM en AWS llamado kiro-user.
El proceso fue:
1.	 Entré a AWS IAM. 
2.	 Fui a Usuarios de IAM y seleccioné Crear persona. 
3.	 Creé el usuario kiro-user. 
4.	 Le asigné permisos mediante la política AdministratorAccess. 
5.	 Entré al usuario y fui a Credenciales de seguridad. 
6.	 Seleccioné Crear clave de acceso. 
7.	 Elegí la opción Servicio de terceros porque Kiro es una aplicación externa que necesita acceder a AWS. 
8.	 AWS generó una Access Key ID y una Secret Access Key. 
9.	En mi equipo Fedora instalé AWS CLI:
sudo dnf install awscli -y
10.	Configuré las credenciales ejecutando:
aws configure
y completé:
AWS Access Key ID: [generada en IAM]
AWS Secret Access Key: [generada en IAM]
Default region name: us-east-1
Default output format: json
11.	 AWS CLI guardó las credenciales en ~/.aws/credentials. 
12.	Kiro detectó automáticamente esas credenciales locales y pudo autenticarse contra AWS.
13.	Finalmente validé la conexión con:
aws sts get-caller-identity
obteniendo el ARN del usuario kiro-user, lo que confirmó que la conexión estaba funcionando.

<img width="878" height="312" alt="Captura desde 2026-05-31 14-56-20" src="https://github.com/user-attachments/assets/0f2de9fc-ace9-4695-8af6-1d01f892099a" />


Resolución de incompatibilidad de Java
Durante el proceso de compilación del proyecto utilizando AWS SAM se presentó un error relacionado con la versión de Java instalada en el sistema.
Al ejecutar:
sam build
AWS SAM mostró el siguiente mensaje:
/usr/bin/mvn is using a JVM with major version 25
which is newer than 17 that is supported by AWS Lambda
El problema se debía a que Fedora tenía instalada la versión Java 25 por defecto, mientras que el proyecto fue desarrollado para ejecutarse sobre Java 17, versión compatible con AWS Lambda.
Para verificar la versión instalada se ejecutó:
java -version
Obteniendo:
openjdk version "25.x"
Para solucionar el problema se instaló Java 17 y se configuró como versión principal del sistema.
Posteriormente se verificó nuevamente la instalación mediante:
java -version
javac -version
mvn -version
Obteniendo como resultado:
Java version: 17.0.19
Con esto se garantizó la compatibilidad entre Maven, AWS SAM y AWS Lambda.
Compilación del proyecto con AWS SAM
Una vez solucionado el problema de compatibilidad de Java, se procedió a compilar el proyecto.
Desde la carpeta raíz del backend se ejecutó:
sam build
AWS SAM analizó el archivo template.yaml, compiló las funciones Lambda y generó los artefactos necesarios para el despliegue.
El proceso finalizó exitosamente mostrando el mensaje:
Build Succeeded
Como resultado se creó la carpeta:
.aws-sam/build
la cual contiene las funciones compiladas y el template procesado listo para ser desplegado en AWS.
Despliegue de la infraestructura en AWS
Después de compilar correctamente el proyecto se ejecutó el despliegue guiado mediante:
sam deploy --guided
Durante el proceso se configuraron los siguientes parámetros:
Stack Name: pos-serverless-aws
AWS Region: us-east-1
Confirm changes before deploy: Yes
Allow SAM CLI IAM role creation: Yes
Disable rollback: No
ProductosFunction has no authentication: Yes
VentasFunction has no authentication: Yes
Save arguments to configuration file: Yes
AWS SAM creó automáticamente un bucket S3 administrado para almacenar los artefactos generados durante la compilación.
Posteriormente generó un Change Set de CloudFormation mostrando todos los recursos que serían creados dentro de AWS.
Luego de aprobar el despliegue, CloudFormation inició la creación de los recursos de infraestructura.
Recursos creados automáticamente
Durante el despliegue se crearon los siguientes componentes:
API Gateway
Se creó una API REST encargada de recibir las solicitudes HTTP provenientes del frontend o de herramientas como Postman.
Endpoints disponibles:
GET /productos
POST /ventas
Funciones Lambda
Se desplegaron dos funciones Lambda:
pos-serverless-aws-productos
pos-serverless-aws-ventas
La primera se encarga de consultar los productos almacenados en DynamoDB y la segunda registra las ventas realizadas.
DynamoDB
Se crearon dos tablas NoSQL:
pos-serverless-aws-productos
pos-serverless-aws-ventas
La tabla Productos almacena la información de los artículos disponibles para la venta y la tabla Ventas registra las transacciones realizadas por los usuarios.
Roles IAM
AWS creó automáticamente los roles y permisos necesarios para permitir la comunicación entre API Gateway, Lambda y DynamoDB.
Verificación del despliegue
Una vez finalizado el proceso, CloudFormation mostró el estado:
Successfully created/updated stack - pos-serverless-aws
Esto confirmó que todos los recursos fueron creados correctamente y que la infraestructura serverless quedó operativa.
URLs generadas por AWS
URL base del API Gateway:
https://bvqj72xsqk.execute-api.us-east-1.amazonaws.com/prod
Endpoint para consultar productos:
https://bvqj72xsqk.execute-api.us-east-1.amazonaws.com/prod/productos
Endpoint para registrar ventas:
https://bvqj72xsqk.execute-api.us-east-1.amazonaws.com/prod/ventas
Estas URLs fueron utilizadas posteriormente para realizar las pruebas en Postman y para conectar el frontend desarrollado en React con el backend desplegado en AWS.
Resultado final
El backend fue compilado, desplegado y publicado exitosamente utilizando AWS SAM, AWS Lambda, API Gateway y DynamoDB, quedando disponible para recibir solicitudes HTTP desde aplicaciones externas y permitiendo validar el funcionamiento completo del sistema POS serverless.

<img width="1467" height="687" alt="Captura desde 2026-05-31 15-17-05" src="https://github.com/user-attachments/assets/2c287e81-a0e1-4c92-b7e3-58091a04f7f8" />

<img width="951" height="945" alt="Captura desde 2026-05-31 15-14-32" src="https://github.com/user-attachments/assets/10bee269-a0e3-4c2f-96f2-7e36905e074f" />

POSTMAN 
<img width="955" height="1141" alt="Captura desde 2026-05-31 15-21-21" src="https://github.com/user-attachments/assets/3f1764ee-eb62-489e-b6b9-272d1ded8be4" />

<img width="961" height="1006" alt="Captura desde 2026-05-31 15-24-34" src="https://github.com/user-attachments/assets/3a9556e1-28ba-4d0e-bfb1-06a5d7fe8705" />


Se mira que se guardo en dynamo en aws
<img width="1920" height="1176" alt="Captura desde 2026-05-31 15-27-32" src="https://github.com/user-attachments/assets/342d9433-0fe6-4e48-8d55-504e5e5c2c95" />



Repositorios GitHub
Frontend:
https://github.com/JeanPierre55/MERCADO-FRONTED
Backend:
https://github.com/JeanPierre55/MERCADO-BACKEND


