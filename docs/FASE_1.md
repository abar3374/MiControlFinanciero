# Fase 1 · Arquitectura — guía paso a paso

## Objetivo

Dejar listo el esqueleto profesional de la app: un proyecto que compila, abre en el teléfono,
muestra el diseño Finance y navega entre las 5 secciones. Todavía no guarda datos: eso empieza
en la Fase 3. Ninguna pantalla tiene botones de mentira; lo que aún no existe lo dice claramente.

## Qué se construyó

| Pieza | Dónde está | Qué hace |
| --- | --- | --- |
| Catálogo de versiones | `gradle/libs.versions.toml` | Todas las versiones de librerías en un solo lugar |
| Lista de módulos | `settings.gradle.kts` | Declara los 17 módulos |
| App | `app/` | `MainActivity`, `FinanceApplication` (Hilt), navegación y barra inferior |
| Navegación | `app/src/main/kotlin/.../navigation/` | Rutas tipadas, barra inferior, cambio de pestañas |
| Diseño | `core/designsystem/` | Colores, tipografía, formas, íconos, `FinanceCard`, `EmptyState`, `ScreenHeader` |
| Dinero | `core/model/.../Money.kt`, `MoneyFormat.kt` | Centavos en `Long` y formato `$1.234.567,89` |
| Clasificaciones | `core/model/.../Classifications.kt` | Fijo/ocasional, naturaleza, frecuencia, métodos de pago, tipo de ingreso |
| Reglas | `core/domain/.../budget/BudgetMath.kt` | Porcentaje usado, restante, balance, umbrales de alerta |
| Inicio | `feature/dashboard/` | Saludo y fecha reales con su ViewModel |
| Perfil | `feature/profile/` | Configuración regional inicial y versión instalada |
| Presupuesto, Registrar, Reportes | `feature/budget`, `feature/expense`, `feature/reports` | Estado vacío con la fase en que llegan |
| Módulos preparados | `core/database`, `data`, `security`, `notifications`, `updates`, `reports`, `feature/credit`, `feature/lock` | Solo configuración; se llenan en su fase |
| Pruebas | `core/model/src/test`, `core/domain/src/test`, `feature/dashboard/src/test` | 14 pruebas automáticas |
| Integración continua | `.github/workflows/ci.yml` | GitHub compila, prueba y deja el APK |

## Dependencias de esta fase

Kotlin 2.1.21 · Android Gradle Plugin 8.10.1 · Gradle 8.14.3 · KSP 2.1.21-2.0.1 ·
Compose BOM 2025.05.00 (Material 3) · Activity Compose 1.10.1 · Lifecycle 2.9.0 ·
Navigation Compose 2.9.0 · kotlinx-serialization 1.8.1 · Hilt 2.56.2 · JUnit 4.13.2.

Android Studio puede sugerir versiones más nuevas. **No las aceptes todavía**: se actualizan
juntas y a propósito, porque deben ser compatibles entre sí.

## Paso 1 · Instalar Android Studio

1. Descarga Android Studio desde https://developer.android.com/studio e instálalo con las
   opciones por defecto ("Standard").
2. Al terminar, el asistente descarga el SDK de Android. Déjalo terminar (puede tardar).

## Paso 2 · Abrir el proyecto

1. Descomprime `MiControlFinanciero.zip` en una carpeta sin espacios ni tildes,
   por ejemplo `C:\Proyectos\MiControlFinanciero` o `~/Proyectos/MiControlFinanciero`.
2. En Android Studio: **File › Open** y elige la carpeta `MiControlFinanciero` (la que contiene `settings.gradle.kts`).
3. Si pregunta "Trust project?", elige **Trust Project**.
4. Abajo aparece **Gradle Sync**. La primera vez descarga Gradle y las librerías (5 a 15 minutos).
   Termina cuando ves *BUILD SUCCESSFUL* o el ícono del martillo queda activo.

## Paso 3 · Compilar

- Menú **Build › Make Project** (o `Ctrl+F9` / `Cmd+F9`).
- Desde la terminal de Android Studio (pestaña *Terminal*):
  - Windows: `gradlew.bat assembleDebug`
  - macOS / Linux: `./gradlew assembleDebug`

## Paso 4 · Ejecutar

**En un emulador:**

1. **Tools › Device Manager › Create Virtual Device** › elige "Pixel 7" › imagen del sistema
   Android 14 o 15 › *Finish*.
2. Arriba, elige ese emulador y presiona **Run ▶** (triángulo verde).

**En tu teléfono:**

1. En el teléfono: *Ajustes › Acerca del teléfono* › toca 7 veces "Número de compilación"
   para activar las Opciones de desarrollador.
2. *Ajustes › Sistema › Opciones de desarrollador* › activa **Depuración por USB**.
3. Conecta el cable USB, acepta el aviso "¿Permitir depuración?" y presiona **Run ▶**.

La app de desarrollo se llama **Finance Dev** y se instala aparte de la versión final.

## Paso 5 · Probar que todo funciona

Pruebas automáticas (terminal):

```bash
./gradlew :core:model:test :core:domain:test testDebugUnitTest
```

Debe terminar en *BUILD SUCCESSFUL*. Los resultados quedan en `*/build/reports/tests/`.

Lista de revisión manual en el teléfono:

- [ ] La app abre en **Inicio** con el saludo correcto para la hora (días, tardes o noches) y la fecha de hoy en español.
- [ ] La barra inferior muestra Inicio, Presupuesto, **Registrar** (círculo azul), Reportes y Perfil.
- [ ] Cada pestaña abre su pantalla; la pestaña activa se ve en azul.
- [ ] Tocar varias veces la misma pestaña no apila pantallas (el botón Atrás no repite).
- [ ] El ícono de persona arriba en Inicio abre **Perfil**.
- [ ] "Registrar un gasto o ingreso" en Inicio abre **Registrar**.
- [ ] Perfil muestra Colombia, COP, `$1.234.567,89`, America/Bogota, Español y la versión `0.1.0-debug (1)`.
- [ ] Girar el teléfono o cambiar el tamaño de letra del sistema no corta textos.
- [ ] La app no pidió ningún permiso.

## Paso 6 · Subir a GitHub (repositorio público)

La forma más sencilla, sin comandos, es **GitHub Desktop**:

1. Crea una cuenta en https://github.com si no tienes, y activa la verificación en dos pasos
   (*Settings › Password and authentication*).
2. Instala GitHub Desktop (https://desktop.github.com) e inicia sesión.
3. *File › Add local repository* › elige la carpeta `MiControlFinanciero` › si dice que no es
   un repositorio, elige **create a repository** aquí mismo.
4. Escribe un resumen como `Fase 1: arquitectura` y presiona **Commit to main**.
5. Presiona **Publish repository**, desmarca *Keep this code private* (público) y publica.
6. En GitHub, pestaña **Actions**: verás el flujo **CI** ejecutándose. Si termina en verde,
   la Fase 1 compila. Dentro de la ejecución, en *Artifacts*, está `finance-debug-apk`.
7. Edita `gradle.properties` y cambia `TU_USUARIO_GITHUB` por tu usuario. Haz commit y push.

Antes de cada commit revisa que no aparezcan archivos `.jks`, `.keystore` ni `local.properties`
(el `.gitignore` ya los excluye).

## Errores conocidos y cómo solucionarlos

| Mensaje o síntoma | Causa | Solución |
| --- | --- | --- |
| `SDK location not found` | Android Studio aún no conoce el SDK | Abre el proyecto desde Android Studio (crea `local.properties` solo) o *File › Project Structure › SDK Location* |
| `Unsupported class file major version` o error de Java | Gradle usa un Java muy viejo | *Settings › Build Tools › Gradle › Gradle JDK* › elige el JDK 17 o 21 que trae Android Studio |
| Sync se queda descargando o falla por red | Conexión lenta o proxy | Reintenta con *File › Sync Project with Gradle Files*; revisa proxy en *Settings › HTTP Proxy* |
| `Failed to find target with hash string 'android-35'` | Falta la plataforma Android 15 | *Tools › SDK Manager* › marca "Android 15 (API 35)" › *Apply* |
| `OutOfMemoryError` / Gradle muy lento | Poca RAM | En `gradle.properties` cambia `-Xmx4g` por `-Xmx3g` |
| `Permission denied: ./gradlew` (macOS / Linux) | El archivo perdió el permiso de ejecución | `chmod +x gradlew` |
| El teléfono no aparece en Android Studio | Falta depuración USB o el cable solo carga | Repite el paso 4 del teléfono; prueba otro cable; en Windows instala el driver USB del fabricante |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Había otra versión firmada distinta | Desinstala "Finance Dev" del teléfono y vuelve a ejecutar |
| Android Studio sugiere actualizar AGP o librerías | Hay versiones más nuevas | Ignóralo por ahora; se actualizan juntas en una fase posterior |

Si aparece un error que no está aquí, copia el mensaje completo (pestaña *Build*) y compártelo.
No avanzamos a la Fase 2 hasta que esta fase compile y abra correctamente.
