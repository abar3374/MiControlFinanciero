# PROJECT_STATUS — Finance (MiControlFinanciero)

> Documento para retomar el desarrollo en una conversación nueva sin perder contexto.
> Pégalo completo al inicio de la nueva conversación junto con el prompt maestro.

**Última actualización:** 2026-11-24
**Versión:** 0.1.0 (versionCode 1)

## Estado actual

| Fase | Nombre | Estado |
| --- | --- | --- |
| 1 | Arquitectura | Compila en GitHub Actions (24-nov, primer intento) · pendiente: el usuario la abre en su teléfono |
| 2 | Interfaz | Pendiente |
| 3 | Base de datos | Pendiente |
| 4 | Presupuestos, gastos e ingresos | Pendiente |
| 5 | Gastos fijos, créditos e ingresos fijos | Pendiente |
| 6 | Notificaciones | Pendiente |
| 7 | Reportes | Pendiente |
| 8 | Seguridad | Pendiente |
| 9 | Actualizaciones | Pendiente |
| 10 | Pruebas y publicación | Pendiente |

**Fase terminada:** Fase 1 compilada y probada en CI; falta la revisión del usuario en el teléfono.
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
| Repositorio público durante el desarrollo | Decisión del usuario; nada secreto en el código |
| Ingresos dentro de `feature/expense` (registro) y `feature/budget` (fijos) | No hace falta un módulo nuevo |
| DEBUG con `applicationIdSuffix ".debug"` | Convive con la versión real |
| Versiones: Kotlin 2.1.21, KSP 2.1.21-2.0.1, AGP 8.10.1, Gradle 8.14.3, Hilt 2.56.2, Compose BOM 2025.05.00 | Combinación estable y compatible |

## Configuración pendiente del usuario

- Repositorio público: https://github.com/abar3374/MiControlFinanciero (creado y conectado a Claude el 24-nov).
- Cada push a `main` publica el APK de prueba como pre-release `v<versionName>-<versionCode>-dev`.
- El APK DEBUG se firma con una clave distinta en cada ejecución de CI: instalar una versión nueva puede
  pedir desinstalar la anterior. **Antes de la Fase 3** (cuando la app empiece a guardar datos) hay que
  configurar una firma fija con GitHub Secrets para no perder datos al actualizar.

## Errores encontrados

- Ninguno. La Fase 1 compiló, pasó las 14 pruebas y el lint en GitHub Actions al primer intento.
- El entorno del asistente no tiene SDK de Android ni puede descargar artefactos de Actions:
  compila siempre en GitHub Actions y entrega el APK por la pre-release.

## Próximas tareas

1. Usuario: instalar el APK de la pre-release v0.1.0-1-dev y revisar la lista de `docs/FASE_1.md` (paso 5).
2. Fase 2 (Interfaz): componentes reutilizables del boceto (tarjetas de resumen, filas de
   movimiento, chips, campos de dinero), estructura visual completa de Inicio y Perfil,
   adaptación a pantallas pequeñas (360 dp) y grandes.
3. Fase 3: Room + SQLCipher, entidades (incluidas `Income` e `IncomeSource`), migraciones y repositorios.
