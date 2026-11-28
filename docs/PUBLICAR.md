# Cómo publicar una versión de Finance

## Versiones

- `versionCode` (entero) **sube siempre** en cada publicación. El actualizador rechaza versiones
  iguales o menores, así que nunca se reutiliza un número.
- `versionName` sigue `MAYOR.MENOR.PARCHE` (por ejemplo, 1.0.1 para una corrección).
- Ambos están en `app/build.gradle.kts`.

## Versión de prueba (canal "Pruebas")

1. Sube `versionCode` (y `versionName` si corresponde) y anota los cambios en `CHANGELOG.md`.
2. Haz push a `main`.
3. GitHub Actions compila, prueba, firma con la clave oficial, verifica la firma y publica la
   pre-release `v<versionName>-<versionCode>-beta` con el APK y su `.sha256`.

## Versión estable (canal "Estable")

1. Verifica que la última versión de prueba funciona en el teléfono.
2. Asegúrate de que `CHANGELOG.md` tiene la sección `## [X.Y.Z]` (se usa como notas).
3. Publica, de una de estas dos formas:
   - **Desde el navegador (recomendado):** GitHub → pestaña **Actions** → **CI** → botón
     **Run workflow** → rama `main` → marca **"Publicar como versión ESTABLE"** → **Run workflow**.
   - **Desde la terminal:** `git tag vX.Y.Z` y `git push origin vX.Y.Z` (igual al `versionName`).
4. GitHub Actions compila, firma, verifica y publica la release **normal** `vX.Y.Z` (marcada como
   *Latest*) con `finance-X.Y.Z-<code>.apk` y su `.sha256`. Si la etiqueta no coincide con
   `versionName`, si ya existe esa versión estable, o si no hay firma oficial, no publica nada.

## Firma (muy importante)

- La clave está cifrada en `signing/finance-release.jks.enc`. Se abre en CI con el secreto
  `SIGNING_KEY_PASSWORD` y se borra del servidor al terminar.
- Huella SHA-256 del certificado:
  `CD:56:E0:E1:D9:36:7C:E8:B3:F8:8A:82:9A:2D:0F:83:93:11:D5:B0:C2:BC:DC:07:61:6F:BC:33:E9:0D:73:38`.
  El actualizador de la app solo acepta APKs con este certificado.
- Guarda la copia de respaldo de la clave (`Finance-respaldo-firma.zip`) y su contraseña fuera de
  GitHub. **Si se pierde la clave, no se podrá actualizar la app sin desinstalarla** (y se pierden
  los datos que no estén en un respaldo `.fbk`).
- Nunca subas la clave sin cifrar, contraseñas ni tokens al repositorio.

## Quién puede publicar

Solo quien tiene permiso de escritura en `abar3374/MiControlFinanciero` puede crear releases.
Aunque alguien publicara un APK en otro lugar, la app lo rechaza: solo descarga desde
`https://github.com/abar3374/MiControlFinanciero/releases/download/` y exige la firma oficial.

## Base de datos

- Cada cambio de esquema sube `version` en `FinanceDatabase` y agrega una `AutoMigration`
  (nunca `fallbackToDestructiveMigration`).
- CI imprime el JSON del esquema nuevo en el log ("===== core/database/schemas/…");
  se copia a `core/database/schemas/` y se sube al repositorio.
