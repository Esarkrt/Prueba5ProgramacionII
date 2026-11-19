package Modelo;

import Excepciones.RutaInvalidaException;
import java.io.*;

public class GestorArchivos {

    private final PersistenciaManager bitacora;

    public GestorArchivos(PersistenciaManager bitacora) {
        this.bitacora = bitacora;
    }

    public void crearCarpeta(File padre, String nombreNuevaCarpeta) throws RutaInvalidaException {
        validarNombre(nombreNuevaCarpeta);
        File nueva = new File(padre, nombreNuevaCarpeta);
        if (nueva.exists()) {
            throw new RutaInvalidaException("La carpeta ya existe: " + nueva.getName());
        }
        if (!nueva.mkdirs()) {
            throw new RutaInvalidaException("No se pudo crear la carpeta. Verifique permisos.");
        }
        bitacora.registrar("CREAR_CARPETA", nueva.getAbsolutePath());
    }

    public void crearArchivoTexto(File padre, String nombreArchivo, String contenido)
            throws RutaInvalidaException, IOException {
        validarNombre(nombreArchivo);
        File destino = new File(padre, nombreArchivo);
        if (destino.exists()) {
            throw new RutaInvalidaException("El archivo ya existe: " + nombreArchivo);
        }
        try (FileWriter writer = new FileWriter(destino)) {
            if (contenido != null) {
                writer.write(contenido);
            } else {
                writer.write("");
            }
        }
        bitacora.registrar("CREAR_ARCHIVO", destino.getAbsolutePath());
    }

    public void renombrar(File actual, String nuevoNombre) throws RutaInvalidaException {
        validarNombre(nuevoNombre);
        File destino = new File(actual.getParentFile(), nuevoNombre);
        if (destino.exists()) {
            throw new RutaInvalidaException("Ya existe un elemento con ese nombre.");
        }
        if (!actual.renameTo(destino)) {
            throw new RutaInvalidaException("No se pudo renombrar el elemento.");
        }
        bitacora.registrar("RENOMBRAR", actual.getAbsolutePath() + " -> " + nuevoNombre);
    }

    public void copiarArchivo(File origen, File destino) throws IOException, RutaInvalidaException {
        if (!origen.exists() || !origen.isFile()) {
            throw new RutaInvalidaException("El archivo de origen no es válido.");
        }
        try (InputStream in = new FileInputStream(origen);
                OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[8192];
            int bytesLeidos;
            while ((bytesLeidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos);
            }
        }
        bitacora.registrar("COPIAR", origen.getAbsolutePath() + " -> " + destino.getAbsolutePath());
    }

    public void moverArchivo(File origen, File destino) throws IOException, RutaInvalidaException {
        if (origen.renameTo(destino)) {
            bitacora.registrar("MOVER", origen.getAbsolutePath() + " -> " + destino.getAbsolutePath());
        } else {
            copiarArchivo(origen, destino);
            if (!origen.delete()) {
                throw new IOException("Se copió el archivo, pero no se pudo eliminar el original.");
            }
            bitacora.registrar("MOVER_COPIA", origen.getAbsolutePath() + " -> " + destino.getAbsolutePath());
        }
    }

    public void eliminarRecursivo(File objetivo) throws IOException {
        if (objetivo.isDirectory()) {
            File[] hijos = objetivo.listFiles();
            if (hijos != null) {
                for (File hijo : hijos) {
                    eliminarRecursivo(hijo);
                }
            }
        }
        if (!objetivo.delete()) {
            throw new IOException("No se pudo eliminar: " + objetivo.getAbsolutePath());
        }
        bitacora.registrar("ELIMINAR", objetivo.getAbsolutePath());
    }

    public boolean esArchivoTextoValido(File archivo) throws IOException {
        try (FileReader reader = new FileReader(archivo)) {
            char[] muestra = new char[1024];
            int leidos = reader.read(muestra);
            for (int i = 0; i < leidos; i++) {
                if (muestra[i] == '\0') {
                    return false;
                }
            }
        }
        return true;
    }

    public String leerTextoConFileReader(File archivo) throws IOException {
        StringBuilder contenido = new StringBuilder();
        try (FileReader fr = new FileReader(archivo);
                BufferedReader br = new BufferedReader(fr)) {
            int caracter;
            while ((caracter = br.read()) != -1) {
                contenido.append((char) caracter);
            }
        }
        bitacora.registrar("ABRIR_TEXTO", archivo.getAbsolutePath());
        return contenido.toString();
    }

    public void guardarTextoConFileWriter(File archivo, String contenido) throws IOException {
        try (FileWriter fw = new FileWriter(archivo, false)) {
            fw.write(contenido);
        }
        bitacora.registrar("EDITAR_TEXTO", archivo.getAbsolutePath());
    }

    private void validarNombre(String nombre) throws RutaInvalidaException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new RutaInvalidaException("El nombre no puede estar vacío.");
        }
        if (nombre.matches(".*[\\\\:*?\"<>|].*")) {
            throw new RutaInvalidaException("El nombre contiene caracteres inválidos.");
        }
    }
}
