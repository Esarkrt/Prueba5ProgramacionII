package modelo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        if (!archivoBitacora.exists()) return "La bitácora está vacía.";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(archivoBitacora))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea).append("\n");
            }
        }
        return sb.toString();
    }

    public void guardarConfiguracion(List<String> claves, List<String> valores) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivoConfig))) {
            for (int i = 0; i < claves.size(); i++) {
                bw.write(claves.get(i) + "=" + valores.get(i));
                bw.newLine();
            }
        }
    }

    public String obtenerValorConfiguracion(String claveBuscada) {
        if (!archivoConfig.exists()) return "";

        try (BufferedReader br = new BufferedReader(new FileReader(archivoConfig))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                int sep = linea.indexOf('=');
                if (sep > 0) {
                    String clave = linea.substring(0, sep);
                    if (clave.equals(claveBuscada)) {
                        return linea.substring(sep + 1);
                    }
                }
            }
        } catch (IOException e) {
            registrar("ERROR_CONFIG", e.getMessage());
        }
        return "";
    }
}