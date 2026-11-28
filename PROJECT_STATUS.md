# PROJECT_STATUS — Finance (MiControlFinanciero)

> Documento para retomar el desarrollo en una conversación nueva sin perder contexto.
> Pégalo completo al inicio de la nueva conversación junto con el prompt maestro.

**Última actualización:** 2026-11-28
**Versión:** 0.9.0 (versionCode 9)

## Estado actual

| Fase | Nombre | Estado |
| --- | --- | --- |
| 1 | Arquitectura | Terminada: compila y el usuario aprobó la interfaz (24-nov) |
| 2 | Interfaz | En curso: personalización pedida por el usuario (perfil, Registrar con tarjetas) |
| 3 | Base de datos | Hecha en lo necesario: esquema v2 (perfil, categorías, rubros, gastos, ingresos, auditoría) |
| 4 | Presupuestos, gastos e ingresos | Terminada (0.3.0; comprobante en 0.5.0) |
| 5 | Gastos fijos, créditos e ingresos fijos | Terminada (0.4.0 – 0.5.0) |
| 6 | Notificaciones | Terminada (0.6.0) |
| 7 | Reportes | Terminada (0.7.0) |
| 8 | Seguridad | Terminada (0.8.0): contraseña, huella, bloqueo configurable, FLAG_SECURE, BD cifrada, respaldo cifrado, auditoría |
| 9 | Actualizaciones | Terminada (0.9.0): GitHub Releases, canales, SHA-256, firma, sin bajar versión |
| 10 | Pruebas y publicación | Pendiente |

**Fase terminada:** Fase 1. El usuario pidió avanzar con perfil, seguridad y Registrar (0.2.0).
**Fase pendiente inmediata:** confirmar Fase 1 → iniciar Fase 2.

## Documentos de diseño

- Propuesta técnica: https://claude.ai/artifact/3KzyVybMsw3GGcdKZ4Cjng
- Boceto de pantallas: https://claude.ai/artifact/XXfqqYdfEkM7WBHWrNp7oW

## Funcionalidades implementadas (Fase 1)

- Proyecto modular de 17 módulos que comparten un catálogo de versiones.
- Tema Finance (Material 3 personalizado) en `core/designsystem`.
- Navegación tipada con barra inferior de 5 accesos; Registrar es el botón central.
- Inicio: saludo por hora + fecha en español, zona America/Bogota (ViewModel con Hilt).
- Perfil: configuración regional inicial y versión instalada (solo lectura).
- Presupuesto, Registrar y Reportes: estado vacío que indica en qué fase llegan.
- `Money` (Long en centavos), `MoneyFormat` (formato colombiano), clasificaciones.
- `BudgetMath` con pruebas (porcentaje, restante, balance, umbrales de alerta).
- Respaldo automático de Android desactivado (`allowBackup=false` + `data_extraction_rules`).
- CI en GitHub Actions: compila, prueba, lint y publica el APK DEBUG como artefacto.

## Versiones 0.5.0 – 0.7.0 (28-nov-2026)
- 0.5.0 (Fase 5): créditos con entidad/tasa/saldo estimado, pagos cada 1–12 meses, ingresos
  esperados, comprobantes adjuntos. BD v4.
- 0.6.0 (Fase 6): recordatorios con WorkManager (diario a la hora elegida), registro de avisos
  enviados para no repetir, notificaciones privadas que abren Pagos del mes.
- 0.7.0 (Fase 7): Reportes por periodo con filtros, gráficas Canvas y exportación PDF / Excel.
  Cálculos en core:domain (ReportCalculator, XlsxWriter) con pruebas; PDF en core:reports.
- 0.8.0 (Fase 8): bloqueo configurable, ocultar pantalla, respaldo cifrado .fbk (BackupCrypto en
  dominio; RoomBackupRepository vuelca todas las tablas a JSON y restaura en una transacción),
  historial de auditoría, AutoLockGuard para selectores/cámara.
- 0.9.0 (Fase 9): core:updates (AppUpdater) + UpdateSelector en dominio. Estable = release normal
  creada al subir la etiqueta vX.Y.Z; Pruebas = pre-releases -beta de cada push a main.

## Versión 0.4.0 (24-nov-2026) — Pagos del mes

Pedido del usuario: "los gastos registrados se deben reiniciar cada mes; si ya se saldó, eliminar".
Se le preguntó y respondió que los gastos deben guardarse y que se le mostrara la mejor opción.
Decisión: **Pagos del mes** — nada se borra; los pagos fijos se renuevan cada mes y los créditos
saldados dejan de aparecer.

- `FixedPaymentSchedule` (dominio, con pruebas): visible si activo, ya empezó y, si es crédito,
  `cuotas pagadas antes del mes < total`. Estado del mes solo por registro de pago.
- Esquema v3: `fixed_payment` (period = año*100+mes), `fixed_payment_record`
  (único por pago y mes, enlaza el gasto creado).
- `RoomFixedPaymentRepository.markPaid` crea el gasto y el registro en una transacción;
  `undoPaid` anula el gasto; `RoomMovementRepository.deleteExpense` borra el registro enlazado.
- Categorías: `addCategory` reactiva una oculta con el mismo nombre; `renameCategory`; `setCategoryHidden`.
- Pantallas en `feature/credit`: `MonthlyPaymentsRoute`, `FixedPaymentFormRoute`.

## Versión 0.3.0 (24-nov-2026) — Fase 4

- Esquema v2 con `AutoMigration(1→2)`; esquemas guardados en `core/database/schemas/` (1.json y 2.json).
- Tablas: expense_category, budget, expense (concept obligatorio, voided = borrado lógico), income, audit_log.
- Repositorios: `RoomCategoryRepository` (siembra 10 categorías si la tabla está vacía),
  `RoomBudgetRepository`, `RoomMovementRepository` (escrituras en transacción con auditoría,
  `observeMonthSummary` combina totales del mes con Flow).
- Rubro automático: al elegir categoría se asigna el primer rubro activo de esa categoría; al escribir
  el concepto, si coincide con el nombre de un rubro y aún no hay categoría, se propone ese rubro.
- Presupuesto global = suma de rubros activos que ya empezaron. Restante = global − gastado en rubros.
  "Te queda" = ingresos − todos los gastos (no es saldo bancario).
- Navegación: formularios y movimientos sin barra inferior (`ExpenseFormDestination(id)`, etc.).
- Textos de las pantallas de la Fase 4 están en el código (español). Pasarlos a strings.xml
  cuando se implemente el cambio de idioma.

## Versión 0.2.0 (24-nov-2026)

- Perfil cifrado: nombre, apellidos, celular, correo, foto (`feature/profile`, `core/data`, `core/database`).
- Usuario y contraseña (hash PBKDF2-SHA256, 120 000 iteraciones) y huella (BIOMETRIC_STRONG).
- `AppLockManager` (`feature/lock`): bloquea al abrir si hay contraseña y tras 60 s en segundo plano.
  Intentos fallidos: desde el 5.º, espera de 30 s que se duplica hasta 15 min (en memoria).
- La contraseña no se puede recuperar (todo es local). Se recomienda activar la huella como segunda vía.
- Registrar: tarjetas "Registrar gasto" e "Registrar ingreso", sin formulario todavía (Fase 4).
- `MainActivity` es `FragmentActivity` (lo exige BiometricPrompt).

## Requisitos agregados por el usuario (24-nov-2026)

1. **"¿En qué gastaste?" obligatorio** en todo gasto, también en "Otros".
   Ejemplo dado: Sistecrédito, $200.465. Se agrega "Lugar o entidad" (opcional).
   Si el concepto coincide con un crédito registrado, proponer registrarlo como pago de cuota.
2. **Ingresos mensuales:** fijos (salario: valor esperado, día de pago, se confirma al recibir)
   y variables (se registran cuando llegan, con origen obligatorio).
3. Inicio muestra **Ingresos vs. gastos** del mes y "Te queda". Reportes agrupa por concepto.
4. Nuevas entidades: `IncomeSource`, `Income`. `Expense` gana `concept` (obligatorio) y `place`.

## Decisiones arquitectónicas

| Decisión | Motivo |
| --- | --- |
| 17 módulos Gradle (app + 9 core + 7 feature) | Elegido por el usuario; separa capas desde el inicio |
| Clean Architecture + MVVM, dominio en Kotlin puro | Reglas de negocio probables sin Android |
| Dinero = `Long` en centavos | Sin errores de redondeo |
| Navegación con rutas `@Serializable` | El compilador detecta rutas rotas |
| Íconos como vectores propios en `designsystem` | Evita la dependencia de íconos de Material |
| Fuente del sistema | Sin descargas, respeta el tamaño de letra del usuario |
| `minSdk 26` (Android 8.0) | Valor por defecto propuesto; el usuario no lo objetó |
| Clave de la base en Keystore (AES-GCM) + SQLCipher | Base ilegible fuera del teléfono |
| Contraseña con PBKDF2 en `core/domain` (Kotlin puro) | Se prueba sin Android |
| Foto reducida a 512 px en `filesDir/profile` | Liviana, privada y sin permisos |
| Clave de firma cifrada en el repo + 1 secreto | El proxy del asistente no puede crear secretos; el usuario solo pega una contraseña |
| Repositorio público durante el desarrollo | Decisión del usuario; nada secreto en el código |
| Ingresos dentro de `feature/expense` (registro) y `feature/budget` (fijos) | No hace falta un módulo nuevo |
| DEBUG con `applicationIdSuffix ".debug"` | Convive con la versión real |
| Versiones: Kotlin 2.1.21, KSP 2.1.21-2.0.1, AGP 8.10.1, Gradle 8.14.3, Hilt 2.56.2, Compose BOM 2025.05.00 | Combinación estable y compatible |

## Configuración pendiente del usuario

- Repositorio público: https://github.com/abar3374/MiControlFinanciero (creado y conectado a Claude el 24-nov).
- Cada push a `main` publica el APK de prueba como pre-release `v<versionName>-<versionCode>-dev`.
- Firma fija: `signing/finance-release.jks.enc` (cifrado AES-256) + secreto `SIGNING_KEY_PASSWORD`.
  Con el secreto, CI publica el APK RELEASE firmado como pre-release `-beta` (app "Finance",
  `com.micontrolfinanciero.app`). Sin el secreto publica el DEBUG `-dev` ("Finance Dev", `.debug`).
  Huella del certificado: CD:56:E0:E1:D9:36:7C:E8:B3:F8:8A:82:9A:2D:0F:83:93:11:D5:B0:C2:BC:DC:07:61:6F:BC:33:E9:0D:73:38.
  El usuario recibió una copia de respaldo de la clave y su contraseña (24-nov).
- Esquemas de Room: CI imprime en el log los JSON nuevos; se copian a `core/database/schemas/`.

## Errores encontrados

- Ninguno. La Fase 1 compiló, pasó las 14 pruebas y el lint en GitHub Actions al primer intento.
- El entorno del asistente no tiene SDK de Android ni puede descargar artefactos de Actions:
  compila siempre en GitHub Actions y entrega el APK por la pre-release.

## Próximas tareas

1. Usuario: instalar 0.4.0 (pre-release v0.4.0-4-beta) y probar pagos del mes, créditos y categorías.
2. Fase 4 pendiente: adjuntar comprobante a un gasto.
3. Fase 5 (resto): ingresos fijos esperados (salario), frecuencias quincenal/bimestral/etc.,
   tasa de interés y saldo de créditos, calendario de pagos. Fase 6: recordatorios 7/3/1/0 días.
4. Fase 2 (Interfaz, pendiente de pulir): componentes reutilizables del boceto (tarjetas de resumen, filas de
   movimiento, chips, campos de dinero), estructura visual completa de Inicio y Perfil,
   adaptación a pantallas pequeñas (360 dp) y grandes.

