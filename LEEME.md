# Paipay DataLogger Android

Aplicación Android nativa en Java para registrar jornadas de acuicultura y movimientos de población en Paipayales. La rama `dev` contiene el desarrollo de v1.4.

## Arquitectura

```text
Android (Java)
  ├─ formularios y semáforo
  ├─ Room: fuente local y cola offline
  ├─ WorkManager: reintentos cuando vuelve la red
  └─ Retrofit HTTPS
          ↓
Django REST Framework en Railway
          ↓
Neon PostgreSQL
```

Android no conoce credenciales de PostgreSQL, no usa Neon Auth y no escribe directamente en Neon. Django es la autoridad de autenticación, permisos, validaciones, auditoría y transacciones.

## Funcionalidad v1.4

- Primer inicio de sesión online con correo y contraseña administrados por Django.
- Sesión local cifrada para poder abrir y trabajar sin conexión después del primer ingreso.
- Catálogo de piscinas descargado desde Django y almacenado en Room.
- Jornadas solo de agua, solo de biometría o mixtas.
- Población estimada obligatoria en toda jornada finalizada.
- Bloque de agua: pH, nitrato, nitrito y amoníaco total. El equipo confirmado
  es un API Freshwater Master Test Kit con escala ppm; los nombres y valores
  discretos de la tarjeta ya forman parte del contrato v1.4.
- Peces anónimos con peso en gramos y longitud total en centímetros.
- Borradores de jornada locales que sobreviven al cierre de la app.
- Movimientos independientes: siembra, mortalidad, cosecha/venta, traslado, escape y ajuste.
- Edición y anulación lógica offline de jornadas y movimientos propios.
- UUID para reintentos idempotentes y versión del servidor para detectar conflictos HTTP `409`.
- Comparación visible de las versiones local/remota ante un `409`, con decisión
  explícita entre descartar el cambio local o reaplicarlo sobre la versión remota.
- Historial móvil limitado a la cuenta activa.
- Semáforo cacheado de la última jornada comunitaria con agua por piscina, sin importar su autor.
- Advertencia de poco espacio y protección de registros pendientes ante almacenamiento lleno.
- Lombricultura visible únicamente como “Próximamente”. Ensayos de laboratorio fuera de v1.4.

## Modelo local

La base nueva se llama `paipay_datalogger_v14.db`. No reutiliza ni transforma las filas de v1.3 porque se confirmó que no existen datos reales que deban migrarse.

Entidades Room principales:

- `JornadaLocal`: cabecera, agua, población, autor, estado y versión.
- `ObservacionPezLocal`: detalle biométrico anónimo de una jornada.
- `MovimientoLocal`: cambio explícito de población independiente de una jornada.
- `PiscinaLocal`: catálogo de piscinas de peces y su especie permanente.
- `SemaforoLocal`: último resumen comunitario descargado para consulta offline.
- `ConflictoLocal`: instantánea remota y operación local original necesarias para
  resolver un `409` sin perder ninguna de las dos versiones.

El esquema Room v2 agrega `ConflictoLocal` mediante una migración `1 → 2` que
conserva jornadas, peces, movimientos y borradores ya guardados en v1.4-dev.

Estados locales relevantes:

- `BORRADOR`: incompleto y nunca enviado.
- `PENDIENTE_CREAR`: completo, debe crearse en Django.
- `PENDIENTE_EDITAR`: corrección con una versión base conocida.
- `PENDIENTE_ANULAR`: anulación lógica por enviar.
- `SINCRONIZADO`: confirmado por Django.
- `CONFLICTO`: Django tiene una versión más reciente; no se sobrescribe automáticamente.
- `ANULADO` / `ANULADO_LOCAL`: registro conservado como histórico, sin participar en análisis.

Room es la fuente visible para los formularios e historial. La interfaz no espera una respuesta de red para guardar.

Al tocar un registro en `CONFLICTO`, la app presenta los valores del teléfono,
los valores del servidor y sus diferencias. “Descartar mi cambio” sustituye la
copia local por la remota. “Reaplicar mi cambio” conserva los datos del teléfono,
actualiza únicamente la versión base y vuelve a poner la operación en cola; si
el servidor cambia otra vez antes del envío, se producirá otro `409`. Un registro
ya anulado en Django no puede reaplicarse porque la anulación es histórica. La
comparación se puede cerrar sin decidir con Atrás o tocando fuera del diálogo.

## Configuración local

Crear `local.properties` en la raíz del repositorio. No debe versionarse:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
API_BASE_URL=https://TU-SERVICIO.up.railway.app/
```

La URL debe terminar en `/`. El valor de ejemplo del proyecto no es un servidor funcional; antes de probar login debe apuntar al despliegue Django real.

Para probar contra Django ejecutándose en la misma computadora que el emulador:

```properties
API_BASE_URL=http://10.0.2.2:PUERTO/
```

`10.0.2.2` es la dirección especial con la que el AVD alcanza al anfitrión. Django
debe escuchar en `0.0.0.0:PUERTO` y aceptar `10.0.2.2` en `ALLOWED_HOSTS`. Solo el
source set `debug` autoriza HTTP hacia ese host; cualquier otro destino HTTP se
rechaza y la variante `release` conserva tráfico en texto claro deshabilitado.
En esta configuración local de depuración la app acepta que Android marque la
red del AVD como no validada si todavía puede alcanzar `10.0.2.2`; producción
continúa exigiendo `NET_CAPABILITY_VALIDATED`.

Requisitos recomendados:

- JDK 17.
- Android SDK Platform 35.
- Android Build Tools 34.0.0 o compatibles con Android Gradle Plugin 8.7.3.
- Gradle 8.9. El repositorio no incluye actualmente los binarios del wrapper, por lo que puede abrirse con Android Studio o usarse una instalación compatible de Gradle.

## Compilación y pruebas

Con Gradle disponible:

```powershell
gradle testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest
```

El APK de depuración queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Las 38 pruebas JVM cubren el semáforo, estados de jornada, reglas de movimientos,
mapeo del contrato API y conectividad de depuración. Las pruebas en
`app/src/androidTest` validan Room y la sesión en un dispositivo o emulador; no
se ejecutan con `testDebugUnitTest`.

No se necesita conectar un teléfono para desarrollar. Las 8 pruebas instrumentadas
se ejecutaron sin fallos en el AVD `Paipay_API_24`, incluidas las migraciones Room
`1 → 2` y `2 → 3`. Sin embargo, antes del uso en Paipayales sí será obligatoria una
prueba de aceptación en un teléfono físico, especialmente para funcionamiento
offline, almacenamiento, formularios biométricos grandes y reconexión.

También se verificó manualmente en ese AVD el recorrido integrado contra Django
local: login, creación de una jornada biométrica sin red, sincronización, edición
offline, respuesta `409` provocada por una corrección concurrente, comparación
de las versiones y reaplicación sobre la versión remota. La creación omite el
campo `version`; las correcciones sí envían la versión positiva conocida.

## Reglas que no deben romperse

1. No inferir mortalidad comparando cuántos peces se midieron en dos jornadas.
2. No cambiar la autoría local al sincronizar con otra cuenta.
3. No sincronizar borradores incompletos.
4. No borrar pendientes para liberar espacio.
5. No sobrescribir un conflicto de versión sin intervención del usuario.
6. No agregar especie a cada pez o movimiento; la especie pertenece permanentemente a la piscina.
7. No incorporar temperatura, oxígeno, laboratorio o variables de lombricultura sin una nueva decisión documentada.
8. No construir SQL con datos externos. Room debe recibirlos mediante parámetros
   DAO y toda escritura remota debe atravesar los serializers/ORM de Django.

El diseño funcional y las razones de arquitectura se mantienen en `../decisiones.md`. El contrato del servidor está en `../PaiPayTech_Django/API.md`.
