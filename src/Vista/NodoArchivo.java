package Vista;

import java.io.File;

public class NodoArchivo {
    private final File archivo;
    private final boolean esRaiz;

    public NodoArchivo(File archivo, boolean esRaiz) {
        this.archivo = archivo;
        this.esRaiz = esRaiz;
    }

    public File getArchivo() {
        return archivo;
    }

    @Override
    public String toString() {
        if (esRaiz) {
            return archivo.getAbsolutePath();
        }
        String prefijo;
        if (archivo.isDirectory()) {
            prefijo = "[Carpeta] ";
        } else {
            prefijo = "[Archivo] ";
        }
        return prefijo + archivo.getName();
    }
}