Archivos ocultos: Se incluyen en el recorrido recursivo y en las estadísticas utilizando File.listFiles(), permitiendo auditar carpetas de configuración o archivos ocultos del sistema de forma transparente.

Archivos sin extensión: Todo archivo sin punto en su nombre o que inicie con punto sin extensión posterior (ej. .gitignore) devuelve cadena vacía "" en obtenerExtension(), clasificándose automáticamente dentro de la categoría Otros.

Enlaces simbólicos: Se previene la recursión infinita almacenando en una lista (rutasVisitadas) la ruta canónica (getCanonicalPath()) de cada directorio visitado antes de descender.
+-----------------------+       +---------------------------+
|   VentanaPrincipal    |<------|   ControladorPrincipal    |
+-----------------------+       +---------------------------+
| - JTree, JTable, etc. |         |              |
+-----------------------+         v              v
                        +-------------------+  +-------------------+
                        |AnalizadorRecursivo|  |  GestorArchivos   |
                        +-------------------+  +-------------------+
                                  |                      |
                                  v                      v
                        +------------------------------------------+
                        |           PersistenciaManager            |
                        +------------------------------------------+
