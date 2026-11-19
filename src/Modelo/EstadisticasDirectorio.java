package Modelo;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstadisticasDirectorio {

    private int cantTxt = 0, cantJava = 0, cantPdf = 0, cantOtros = 0;
    private long bytesTxt = 0, bytesJava = 0, bytesPdf = 0, bytesOtros = 0;

    private File archivoMasGrande = null;
    private File archivoMasReciente = null;
    private int profundidadMaxima = 0;
    private int totalCarpetas = 0;
    private int carpetasVacias = 0;
    private Map<String, List<File>> duplicados = new HashMap<>();

    public void registrarArchivo(File archivo, String ext, long tamano) {
        switch (ext) {
            case "txt":
                cantTxt++;
                bytesTxt += tamano;
                break;
            case "java":
                cantJava++;
                bytesJava += tamano;
                break;
            case "pdf":
                cantPdf++;
                bytesPdf += tamano;
                break;
            default:
                cantOtros++;
                bytesOtros += tamano;
                break;
        }

        if (archivoMasGrande == null || tamano > archivoMasGrande.length()) {
            archivoMasGrande = archivo;
        }
        if (archivoMasReciente == null || archivo.lastModified() > archivoMasReciente.lastModified()) {
            archivoMasReciente = archivo;
        }
    }

    public void filtrarYAsignarDuplicados(Map<String, List<File>> todosLosGrupos) {
        duplicados.clear();
        for (Map.Entry<String, List<File>> entrada : todosLosGrupos.entrySet()) {
            if (entrada.getValue().size() > 1) {
                duplicados.put(entrada.getKey(), entrada.getValue());
            }
        }
    }

    public int getTotalArchivos() {
        return cantTxt + cantJava + cantPdf + cantOtros;
    }

    public long getTotalBytes() {
        return bytesTxt + bytesJava + bytesPdf + bytesOtros;
    }

    public double calcularPorcentaje(int cantidad) {
        int total = getTotalArchivos();
        if (total == 0) {
            return 0.0;
        }
        return (cantidad * 100.0) / total;
    }

    public static String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.2f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    public int getCantTxt() {
        return cantTxt;
    }

    public int getCantJava() {
        return cantJava;
    }

    public int getCantPdf() {
        return cantPdf;
    }

    public int getCantOtros() {
        return cantOtros;
    }

    public long getBytesTxt() {
        return bytesTxt;
    }

    public long getBytesJava() {
        return bytesJava;
    }

    public long getBytesPdf() {
        return bytesPdf;
    }

    public long getBytesOtros() {
        return bytesOtros;
    }

    public File getArchivoMasGrande() {
        return archivoMasGrande;
    }

    public File getArchivoMasReciente() {
        return archivoMasReciente;
    }

    public int getProfundidadMaxima() {
        return profundidadMaxima;
    }

    public void setProfundidadMaxima(int profundidadMaxima) {
        this.profundidadMaxima = profundidadMaxima;
    }

    public int getTotalCarpetas() {
        return totalCarpetas;
    }

    public void incrementarTotalCarpetas() {
        this.totalCarpetas++;
    }

    public int getCarpetasVacias() {
        return carpetasVacias;
    }

    public void incrementarCarpetasVacias() {
        this.carpetasVacias++;
    }

    public Map<String, List<File>> getDuplicados() {
        return duplicados;
    }
}
