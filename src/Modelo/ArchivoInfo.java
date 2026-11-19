package Modelo;

import java.io.File;

public class ArchivoInfo {

    private final File archivo;
    private final String nombre;
    private final String rutaAbsoluta;
    private final long tamanoBytes;
    private final long fechaModificacion;

    public ArchivoInfo(File archivo) {
        this.archivo = archivo;
        this.nombre = archivo.getName();
        this.rutaAbsoluta = archivo.getAbsolutePath();
        this.tamanoBytes = archivo.length();
        this.fechaModificacion = archivo.lastModified();
    }

    public File getArchivo() {
        return archivo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRutaAbsoluta() {
        return rutaAbsoluta;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public long getFechaModificacion() {
        return fechaModificacion;
    }
}
