package Modelo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PersistenciaManager {

    private final File archivoBitacora = new File("bitacora.log");
    private final File archivoConfig = new File("config.txt");
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public synchronized void registrar(String accion, String ruta) {
        String linea = String.format("[%s] %-15s | %s%n",
                formatoFecha.format(new Date()), accion, ruta);
        try (FileWriter fw = new FileWriter(archivoBitacora, true);
                BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(linea);
        } catch (IOException e) {
            System.err.println("Error escribiendo en bitácora: " + e.getMessage());
        }
    }

    public String leerBitacoraCompleta() throws IOException {
        if (!archivoBitacora.exists()) {
            return "La bitácora está vacía.";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(archivoBitacora))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea).append("\n");
            }
        }
        return sb.toString();
    }

    public void guardarConfiguracion(Map<String, String> parametros) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivoConfig))) {
            for (Map.Entry<String, String> entry : parametros.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue());
                bw.newLine();
            }
        }
    }

    public Map<String, String> cargarConfiguracion() {
        Map<String, String> config = new HashMap<>();
        if (!archivoConfig.exists()) {
            return config;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivoConfig))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                int sep = linea.indexOf('=');
                if (sep > 0) {
                    config.put(linea.substring(0, sep), linea.substring(sep + 1));
                }
            }
        } catch (IOException e) {
            registrar("ERROR_CONFIG", e.getMessage());
        }
        return config;
    }

    public void exportarReporte(File destino, EstadisticasDirectorio stats, String rutaAnalizada, boolean esCsv) throws IOException {
        String fechaHora = formatoFecha.format(new Date());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(destino))) {
            if (esCsv) {
                bw.write("FechaReporte,RutaAnalizada,Categoria,Cantidad,TamanoBytes,TamanoLegible,Porcentaje");
                bw.newLine();
                escribirFilaCsv(bw, fechaHora, rutaAnalizada, ".txt", stats.getCantTxt(), stats.getBytesTxt(), stats.calcularPorcentaje(stats.getCantTxt()));
                escribirFilaCsv(bw, fechaHora, rutaAnalizada, ".java", stats.getCantJava(), stats.getBytesJava(), stats.calcularPorcentaje(stats.getCantJava()));
                escribirFilaCsv(bw, fechaHora, rutaAnalizada, ".pdf", stats.getCantPdf(), stats.getBytesPdf(), stats.calcularPorcentaje(stats.getCantPdf()));
                escribirFilaCsv(bw, fechaHora, rutaAnalizada, "Otros", stats.getCantOtros(), stats.getBytesOtros(), stats.calcularPorcentaje(stats.getCantOtros()));
            } else {
                bw.write("=== REPORTE DE ANÁLISIS DE DIRECTORIO ===");
                bw.newLine();
                bw.write("Fecha y Hora: " + fechaHora);
                bw.newLine();
                bw.write("Ruta Analizada: " + rutaAnalizada);
                bw.newLine();
                bw.newLine();
                bw.write(String.format(".txt  : %d archivos | %s | %.2f%%%n", stats.getCantTxt(), EstadisticasDirectorio.formatearTamano(stats.getBytesTxt()), stats.calcularPorcentaje(stats.getCantTxt())));
                bw.write(String.format(".java : %d archivos | %s | %.2f%%%n", stats.getCantJava(), EstadisticasDirectorio.formatearTamano(stats.getBytesJava()), stats.calcularPorcentaje(stats.getCantJava())));
                bw.write(String.format(".pdf  : %d archivos | %s | %.2f%%%n", stats.getCantPdf(), EstadisticasDirectorio.formatearTamano(stats.getBytesPdf()), stats.calcularPorcentaje(stats.getCantPdf())));
                bw.write(String.format("Otros : %d archivos | %s | %.2f%%%n", stats.getCantOtros(), EstadisticasDirectorio.formatearTamano(stats.getBytesOtros()), stats.calcularPorcentaje(stats.getCantOtros())));
                bw.newLine();
                bw.write("Total carpetas: " + stats.getTotalCarpetas() + " (Vacías: " + stats.getCarpetasVacias() + ")");
                bw.newLine();
                bw.write("Profundidad máxima: " + stats.getProfundidadMaxima());
                bw.newLine();

                String rutaGrande = "N/A";
                if (stats.getArchivoMasGrande() != null) {
                    rutaGrande = stats.getArchivoMasGrande().getAbsolutePath();
                }
                bw.write("Archivo más grande: " + rutaGrande);
                bw.newLine();

                String rutaReciente = "N/A";
                if (stats.getArchivoMasReciente() != null) {
                    rutaReciente = stats.getArchivoMasReciente().getAbsolutePath();
                }
                bw.write("Archivo más reciente: " + rutaReciente);
                bw.newLine();
            }
        }
        registrar("EXPORTAR_REPORTE", destino.getAbsolutePath());
    }

    private void escribirFilaCsv(BufferedWriter bw, String fecha, String ruta, String cat, int cant, long bytes, double pct) throws IOException {
        bw.write(String.format("\"%s\",\"%s\",\"%s\",%d,%d,\"%s\",%.2f",
                fecha, ruta, cat, cant, bytes, EstadisticasDirectorio.formatearTamano(bytes), pct));
        bw.newLine();
    }
}
