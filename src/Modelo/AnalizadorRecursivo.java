package modelo;

import excepciones.RutaInvalidaException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalizadorRecursivo {

    private final PersistenciaManager bitacora;
    private final List<String> rutasVisitadas;
    private volatile boolean cancelado = false;

    public AnalizadorRecursivo(PersistenciaManager bitacora) {
        this.bitacora = bitacora;
        this.rutasVisitadas = new ArrayList<>();
    }

    public void cancelar() {
        this.cancelado = true;
    }

    public void reiniciarEstado() {
        this.cancelado = false;
        this.rutasVisitadas.clear();
    }

    public File validarDirectorio(String ruta) throws RutaInvalidaException {
        if (ruta == null || ruta.trim().isEmpty()) {
            throw new RutaInvalidaException("La ruta no puede estar vacía.");
        }
        File dir = new File(ruta.trim());
        if (!dir.exists()) {
            throw new RutaInvalidaException("La ruta especificada no existe: " + ruta);
        }
        if (!dir.isDirectory()) {
            throw new RutaInvalidaException("La ruta no corresponde a un directorio: " + ruta);
        }
        return dir;
    }

    public EstadisticasDirectorio analizar(File raiz, int profundidadMax) {
        reiniciarEstado();
        EstadisticasDirectorio stats = new EstadisticasDirectorio();
        List<GrupoDuplicados> listaGrupos = new ArrayList<>();

        recorrerEstadisticas(raiz, 0, profundidadMax, stats, listaGrupos);
        stats.filtrarYAsignarDuplicados(listaGrupos);
        return stats;
    }

    private void recorrerEstadisticas(File actual, int nivel, int profundidadMax,
                                      EstadisticasDirectorio stats,
                                      List<GrupoDuplicados> listaGrupos) {
        if (cancelado) return;
        if (profundidadMax > 0 && nivel > profundidadMax) return;

        try {
            String canonica = actual.getCanonicalPath();
            if (rutasVisitadas.contains(canonica)) {
                return;
            }
            rutasVisitadas.add(canonica);
        } catch (IOException e) {
            bitacora.registrar("ERROR_CANONICAL", actual.getAbsolutePath());
            return;
        }

        if (nivel > stats.getProfundidadMaxima()) {
            stats.setProfundidadMaxima(nivel);
        }

        File[] hijos = actual.listFiles();
        if (hijos == null) {
            bitacora.registrar("SIN_PERMISO", actual.getAbsolutePath());
            return;
        }

        if (hijos.length == 0 && nivel > 0) {
            stats.incrementarCarpetasVacias();
        }

        for (File hijo : hijos) {
            if (cancelado) return;

            if (hijo.isDirectory()) {
                stats.incrementarTotalCarpetas();
                recorrerEstadisticas(hijo, nivel + 1, profundidadMax, stats, listaGrupos);
            } else if (hijo.isFile()) {
                procesarArchivoEstadistica(hijo, stats, listaGrupos);
            }
        }
    }

    private void procesarArchivoEstadistica(File archivo, EstadisticasDirectorio stats,
                                            List<GrupoDuplicados> listaGrupos) {
        long tamano = archivo.length();
        String nombre = archivo.getName();
        String ext = obtenerExtension(nombre);

        stats.registrarArchivo(archivo, ext, tamano);

        GrupoDuplicados grupoEncontrado = null;
        for (GrupoDuplicados grupo : listaGrupos) {
            if (grupo.getNombre().equals(nombre) && grupo.getTamano() == tamano) {
                grupoEncontrado = grupo;
                break;
            }
        }

        if (grupoEncontrado == null) {
            grupoEncontrado = new GrupoDuplicados(nombre, tamano);
            listaGrupos.add(grupoEncontrado);
        }
        grupoEncontrado.agregarArchivo(archivo);
    }

    public String obtenerExtension(String nombreArchivo) {
        int ultimoPunto = nombreArchivo.lastIndexOf('.');
        if (ultimoPunto <= 0 || ultimoPunto == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(ultimoPunto + 1).toLowerCase();
    }
}