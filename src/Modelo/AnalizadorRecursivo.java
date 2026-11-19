package Modelo;

import Excepciones.RutaInvalidaException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, List<File>> mapaDuplicados = new HashMap<>();

        recorrerEstadisticas(raiz, 0, profundidadMax, stats, mapaDuplicados);
        stats.filtrarYAsignarDuplicados(mapaDuplicados);
        return stats;
    }

    private void recorrerEstadisticas(File actual, int nivel, int profundidadMax,
            EstadisticasDirectorio stats,
            Map<String, List<File>> mapaDuplicados) {
        if (cancelado) {
            return;
        }
        if (profundidadMax > 0 && nivel > profundidadMax) {
            return;
        }

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
            if (cancelado) {
                return;
            }

            if (hijo.isDirectory()) {
                stats.incrementarTotalCarpetas();
                recorrerEstadisticas(hijo, nivel + 1, profundidadMax, stats, mapaDuplicados);
            } else if (hijo.isFile()) {
                procesarArchivoEstadistica(hijo, stats, mapaDuplicados);
            }
        }
    }

    private void procesarArchivoEstadistica(File archivo, EstadisticasDirectorio stats,
            Map<String, List<File>> mapaDuplicados) {
        long tamano = archivo.length();
        String nombre = archivo.getName();
        String ext = obtenerExtension(nombre);

        stats.registrarArchivo(archivo, ext, tamano);

        String clave = nombre + "::" + tamano;
        if (!mapaDuplicados.containsKey(clave)) {
            mapaDuplicados.put(clave, new ArrayList<>());
        }
        mapaDuplicados.get(clave).add(archivo);
    }

    public List<ArchivoInfo> buscar(File raiz, FiltroBusqueda filtro) {
        reiniciarEstado();
        List<ArchivoInfo> resultados = new ArrayList<>();
        recorrerBusqueda(raiz, 0, filtro, resultados);
        bitacora.registrar("BUSCAR", raiz.getAbsolutePath() + " (Texto: '" + filtro.getTexto() + "')");
        return resultados;
    }

    private void recorrerBusqueda(File actual, int nivel, FiltroBusqueda filtro, List<ArchivoInfo> resultados) {
        if (cancelado) {
            return;
        }
        if (filtro.getProfundidadMax() > 0 && nivel > filtro.getProfundidadMax()) {
            return;
        }

        try {
            String canonica = actual.getCanonicalPath();
            if (rutasVisitadas.contains(canonica)) {
                return;
            }
            rutasVisitadas.add(canonica);
        } catch (IOException e) {
            return;
        }

        File[] hijos = actual.listFiles();
        if (hijos == null) {
            return;
        }

        for (File hijo : hijos) {
            if (cancelado) {
                return;
            }

            if (hijo.isDirectory()) {
                recorrerBusqueda(hijo, nivel + 1, filtro, resultados);
            } else if (hijo.isFile()) {
                if (cumpleFiltros(hijo, filtro)) {
                    resultados.add(new ArchivoInfo(hijo));
                }
            }
        }
    }

    private boolean cumpleFiltros(File archivo, FiltroBusqueda filtro) {
        String nombre = archivo.getName().toLowerCase();
        if (!filtro.getTexto().isEmpty() && !nombre.contains(filtro.getTexto())) {
            return false;
        }
        if (!filtro.getExtension().isEmpty()) {
            String ext = obtenerExtension(nombre);
            if (!ext.equals(filtro.getExtension())) {
                return false;
            }
        }
        long tamano = archivo.length();
        if (tamano < filtro.getTamanoMinBytes()) {
            return false;
        }
        if (filtro.getTamanoMaxBytes() > 0 && tamano > filtro.getTamanoMaxBytes()) {
            return false;
        }

        long mod = archivo.lastModified();
        if (filtro.getFechaMinMillis() > 0 && mod < filtro.getFechaMinMillis()) {
            return false;
        }
        if (filtro.getFechaMaxMillis() > 0 && mod > filtro.getFechaMaxMillis()) {
            return false;
        }

        return true;
    }

    public String obtenerExtension(String nombreArchivo) {
        int ultimoPunto = nombreArchivo.lastIndexOf('.');
        if (ultimoPunto <= 0 || ultimoPunto == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(ultimoPunto + 1).toLowerCase();
    }
}
