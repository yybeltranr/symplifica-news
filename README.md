# Symplifica News

Backend que consulta el RSS de entretenimiento de Portafolio, identifica noticias nuevas mediante el GUID del feed, genera un resumen en español con un modelo local de Ollama integrado mediante LangChain4j, envía el resultado por correo HTML y registra las ejecuciones en PostgreSQL.

## 1. Funcionalidad general

El proceso realiza estas tareas:

1. Consulta el RSS de entretenimiento de Portafolio.
2. Compara los GUID recibidos con los que ya están registrados en `news_items`.
3. Conserva únicamente las noticias que todavía no se han procesado.
4. Envía los titulares de esas noticias a Ollama mediante LangChain4j.
5. Solicita al modelo un resumen en español de máximo tres párrafos, destacando las noticias relevantes.
6. Inserta el resumen en una plantilla HTML y lo envía por SMTP a uno o más destinatarios configurados.
7. Registra el resultado de la ejecución y las noticias procesadas en PostgreSQL.

La aplicación no envía correo cuando el RSS no contiene noticias nuevas.

## 2. Arquitectura

El proyecto está organizado por responsabilidades:

| Paquete | Responsabilidad |
|---|---|
| `config` | Configuración de Ollama y propiedades de la aplicación. |
| `controller` | Endpoints REST para consultar historial, noticias y ejecutar el proceso manualmente. |
| `dto` | Objetos de respuesta de la API. |
| `entity` | Entidades JPA `NewsLog` y `NewsItem`. |
| `exception` | Manejo global de errores REST mediante `ProblemDetail`. |
| `job` | Trabajo recurrente administrado por JobRunr. |
| `repository` | Acceso a datos mediante Spring Data JPA. |
| `service/ai` | Generación del resumen mediante LangChain4j y Ollama. |
| `service/email` | Construcción del correo HTML y envío SMTP. |
| `service/logging` | Registro y finalización de ejecuciones. |
| `service/rss` | Descarga y análisis del feed RSS. |

Flujo de ejecución:

```text
JobRunr (@Recurring)
       |
       v
NewsOrchestrationService
       |
       +--> RssService --> RSS de Portafolio
       |
       +--> NewsItemRepository --> consulta GUID registrados
       |
       +--> filtra noticias nuevas
       |
       +--> AiSummaryService --> LangChain4j --> Ollama (Gemma 3 4B)
       |                         recibe los titulares
       |
       +--> EmailService --> plantilla HTML --> servidor SMTP
       |
       +--> NewsItemRepository --> guarda noticias procesadas
       |
       +--> NewsLogService --> registra SUCCESS / FAILED
                         |
                         v
                    PostgreSQL
```

## 3. Tecnologías y requisitos

- Java JDK 25.
- Spring Boot 4.1.2.
- Maven o Maven Wrapper.
- PostgreSQL.
- Flyway para versionamiento del esquema.
- JobRunr 9.0.1 para la ejecución recurrente y su Dashboard.
- LangChain4j 1.23.0 para integrar el modelo local.
- Ollama con el modelo `gemma3:4b`.
- SMTP para enviar los correos.

Descargar el modelo local si todavía no está instalado:

```bash
ollama pull gemma3:4b
ollama list
curl http://localhost:11434/api/tags
```

## 4. Configuración

Crear un archivo `.env` en la raíz del proyecto, al mismo nivel de `pom.xml`, tomando como referencia `.env.example`.

Ejemplo de configuración:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/symplifica_news
DB_USERNAME=symplifica_app
DB_PASSWORD=TU_CONTRASEÑA

OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=gemma3:4b

MAIL_HOST=tu_servidor_smtp
MAIL_PORT=tu_puerto_smtp
MAIL_USERNAME=remitente@gmail.com
MAIL_PASSWORD=CLAVE_DE_APLICACION

NEWS_EMAIL_TO=destino1@example.com,destino2@example.com
NEWS_JOB_CRON=0 * * * *
```

`NEWS_EMAIL_TO` admite varios destinatarios separados por coma o punto y coma. Todos reciben el mismo mensaje en un solo envío. El asunto incluye una marca temporal para diferenciar los envíos sucesivos en el cliente de correo; la forma en que se agrupan los mensajes también depende del cliente utilizado (actualmente -> configurado para Google).

`MAIL_USERNAME` identifica al remitente. `MAIL_PASSWORD` debe contener la credencial SMTP correspondiente. Para Gmail, cuando aplique, se debe utilizar una contraseña de aplicación y no la contraseña habitual de la cuenta.

**Seguridad:** no incluir credenciales reales en el repositorio ni compartir el archivo `.env`. El archivo `.env.example` debe contener únicamente valores ilustrativos.

### Frecuencia del trabajo

La propiedad `NEWS_JOB_CRON` controla la frecuencia de JobRunr. El trabajo usa la zona horaria `America/Bogota`.

| Valor | Frecuencia | Uso |
|---|---|---|
| `* * * * *` | Cada minuto | Pruebas temporales. |
| `0 * * * *` | Cada hora, al minuto cero | Configuración recomendada para la operación normal. |

Después de cambiar la variable de entorno se debe reiniciar la aplicación para que la configuración del trabajo recurrente se actualice.

La frecuencia del job no controla la publicación del RSS. El job consulta el feed según su cron, pero Portafolio incorpora noticias cuando el medio actualiza su contenido. Por eso una consulta cada minuto no implica que exista una noticia nueva cada minuto.

## 5. Ejecución local

Iniciar PostgreSQL y Ollama. 
```
CREATE DATABASE symplifica_news;

CREATE USER symplifica_app
WITH PASSWORD 'tu_password';

GRANT ALL PRIVILEGES
ON DATABASE symplifica_news
TO symplifica_app;
```

Posteriormente, deben configurarse `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

Verificar que la base de datos y las credenciales estén configuradas. Desde la raíz del proyecto, ejecutar:

```bash
mvn clean test
mvn spring-boot:run
```

Con Maven Wrapper:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Spring Boot ejecuta las migraciones Flyway al iniciar. JobRunr guarda la información de los trabajos en PostgreSQL. El Dashboard de JobRunr está disponible localmente en:

`http://localhost:8000/dashboard`

## 6. Persistencia y deduplicación

### Tabla `news_log`

Registra cada ejecución del proceso, incluyendo fechas de inicio y finalización, estado, cantidad de noticias, resumen y mensaje de error cuando corresponda.

### Tabla `news_items`

Registra las noticias procesadas: GUID, título, enlace, descripción, fecha de publicación, categoría, imagen, fecha de procesamiento y referencia a la ejecución que las procesó.

El GUID del RSS es la clave funcional para reconocer noticias que ya fueron procesadas. La base de datos también aplica una restricción única sobre ese campo.

Cuando una ejecución no encuentra noticias nuevas:

- queda registrada en `news_log` con estado `SUCCESS`;
- registra `newsCount = 0`;
- no llama al modelo de IA;
- no envía correo.

Cuando hay noticias nuevas, el sistema genera el resumen y envía el correo. Las noticias se guardan después de que el envío se completa.

### Migraciones Flyway

- `V1`: creación de la tabla `users`.
- `V2`: creación de `news_log`.
- `V3`: permite el estado transitorio `RUNNING`.
- `V4`: creación de `news_items`, relación e índices.

## 7. Resumen con Ollama y correo HTML

`AiSummaryService` utiliza LangChain4j para invocar el modelo local `gemma3:4b`. En la versión final del prompt descrita para este proyecto, se envían los **titulares** de las noticias nuevas, no sus descripciones. Se solicita un resumen en español, de máximo tres párrafos, que destaque las noticias relevantes.

El modelo puede omitir titulares que considere menos relevantes. Las instrucciones del prompt buscan evitar invenciones y mantener el sentido de los titulares, pero no garantizan exactitud periodística ni obediencia perfecta al formato. El resumen generado debe considerarse contenido automatizado y puede requerir revisión.

`EmailService` construye el cuerpo HTML, escapa el contenido recibido para evitar que se interprete como HTML proporcionado por el modelo y lo envía por SMTP. Los destinatarios se leen de `NEWS_EMAIL_TO`. El asunto base se toma de la configuración de la aplicación y se complementa con una marca temporal para distinguir envíos.

## 8. API REST

La API utiliza el prefijo `/api/news`:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/news/logs` | Consulta las últimas 50 ejecuciones. |
| `GET` | `/api/news/logs/{id}` | Consulta el detalle de una ejecución. |
| `GET` | `/api/news/items` | Consulta las últimas 100 noticias procesadas. |
| `POST` | `/api/news/run` | Ejecuta manualmente el flujo de forma síncrona. |

Ejemplo:

```bash
curl http://localhost:8080/api/news/logs
```

**Precaución:** `POST /api/news/run` ejecuta el flujo real y puede enviar un correo a todos los destinatarios configurados. Evitar llamadas manuales simultáneas con la ejecución programada durante las pruebas.

Los errores de la API se centralizan mediante el manejador global y respuestas `ProblemDetail`.

## 9. Pruebas

Ejecutar la suite con:

```bash
mvn clean test
```

Durante el desarrollo se reportó una ejecución satisfactoria de 20 pruebas, sin fallos ni errores (`BUILD SUCCESS`). Después de los últimos ajustes al prompt de IA y al asunto dinámico del correo, se recomienda volver a ejecutar la suite completa antes de entregar la versión definitiva.

Las pruebas unitarias utilizan JUnit 5 y Mockito para simular dependencias. La prueba de contexto deshabilita el scheduler, el servidor de trabajos y el Dashboard de JobRunr para evitar ejecuciones recurrentes durante los tests.

Al actualizar las pruebas, considerar especialmente:

- `AiSummaryServiceTest`: comprobar que el prompt contiene los titulares y no las descripciones, y que la respuesta no esté vacía.
- `EmailServiceTest`: comprobar el destinatario, el contenido HTML y que el asunto comience con el asunto base configurado, ya que ahora incluye una marca temporal.

## 10. Limitaciones y consideraciones

- **Exactitud del resumen:** un modelo generativo puede interpretar incorrectamente un titular, alterar el grado de certeza o incluir inferencias. La revisión editorial sigue siendo recomendable.
- **Concurrencia:** el flujo consulta GUID y los guarda después del envío. Si una ejecución manual y otra programada procesan simultáneamente las mismas noticias, ambas podrían superar la consulta inicial antes de que se persistan los GUID. Evitar ejecuciones simultáneas; para un entorno de producción se recomienda incorporar un mecanismo de bloqueo o reserva transaccional.
- **Correo y base de datos:** SMTP y PostgreSQL no participan en una transacción distribuida. Si el correo se entrega y posteriormente falla el guardado, una ejecución futura podría reenviar esas noticias.
- **Seguridad de API:** los endpoints son apropiados para un entorno local/demo. Antes de exponerlos públicamente, agregar autenticación, autorización y controles de acceso.
- **Usuarios:** el esquema incluye la tabla `users`, pero el proyecto descrito no implementa un flujo de autenticación ni administración de usuarios.
