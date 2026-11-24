# Registro de cambios

Formato: cada versión lista lo agregado, cambiado y corregido. La más reciente va arriba.

## [0.1.0] - 2026-11-24 · Fase 1: arquitectura

### Agregado
- Proyecto Android modular: `app`, 9 módulos `core` y 7 módulos `feature`.
- Tema visual Finance (paleta blanco, gris claro, azul, azul oscuro y gris medio).
- Barra inferior con Inicio, Presupuesto, Registrar (botón central), Reportes y Perfil.
- Inicio con saludo según la hora y fecha en español (zona America/Bogota).
- Perfil con la configuración regional inicial (Colombia, COP, `$1.234.567,89`) y versión instalada.
- Pantallas con estado vacío que explican en qué fase llega cada función.
- `Money` (centavos en `Long`) y `MoneyFormat` con pruebas unitarias.
- `BudgetMath`: porcentaje consumido, presupuesto restante, balance ingresos − gastos y umbrales 50/75/90/100 %.
- Clasificaciones: frecuencia, naturaleza, métodos de pago, tipo de ingreso (fijo / variable).
- Copias de seguridad automáticas de Android desactivadas para proteger los datos.
- GitHub Actions: compilación, pruebas, lint y APK de desarrollo descargable.
