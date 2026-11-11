# Arquitectura de la biblioteca Android

La biblioteca usa la misma composición que iOS: `AppHeader`, `LibrarySectionNavigation`, contenido de la sección seleccionada y `MiniPlayer`. `LibraryScreen` crea las piezas compartidas una vez y solo cambia el contenido central al navegar entre Canciones, Favoritos, Listas, Álbumes y Artistas. `PlayerViewModel` sigue siendo la fuente común de reproducción.

`LibraryComponent` identifica cada tipo de pieza y publica sus variantes disponibles. Cada variante tiene un ID de texto estable. `PlayerTheme.componentDefaults` define el diseño inicial de cada componente en el tema Neón o Aurora. `ThemeViewModel` guarda las elecciones personalizadas en `SharedPreferences`, por componente, y las aplica por encima del tema base. La pantalla Temas permite mezclar los diseños y volver al valor del tema completo.

Las piezas están en carpetas dentro de `app/src/main/java/mx/musicalesdoris/reproductorlocalandroid/library/components/`. Cada carpeta contiene un selector pequeño y un archivo por diseño. Por ejemplo, `album/AlbumCard.kt` llama a `NeonAlbumCard.kt` o `AuroraAlbumCard.kt` según el ID. Las acciones y el estado no se duplican entre variantes: `SongRowActions` concentra favorito y eliminación, y `MiniPlayerContent` concentra progreso y reproducción.

Para añadir una variante de álbum: crea un archivo nuevo en `components/album/`, registra su ID y nombre en `LibraryComponent.ALBUM.availableVariants`, añade el caso en `AlbumCard.kt` y, si debe ser el diseño inicial de un tema, actualiza `PlayerTheme.componentDefaults`. Los demás componentes no necesitan cambios.

Canciones y Favoritos usan el catálogo real. Listas, Álbumes y Artistas tienen modelos de presentación y variantes visuales preparadas, pero siguen vacíos hasta que `Song` y `SongLibrary` almacenen los metadatos y relaciones necesarios. No se deduce el álbum o artista a partir del nombre del archivo.
