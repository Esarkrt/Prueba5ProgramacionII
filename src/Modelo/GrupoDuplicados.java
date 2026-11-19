package modelo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GrupoDuplicados {
    private final String nombre;
    private final long tamano;
    private final List<File> archivos;

    public GrupoDuplicados(String nombre, long tamano) {
        this.nombre = nombre;
        this.tamano = tamano;
        this.archivos = new ArrayList<>();
    }

    public String getNombre() {
        return nombre;
    }

    public long getTamano() {
        return tamano;
    }

    public List<File> getArchivos() {
        return archivos;
    }

    public void agregarArchivo(File archivo) {
        archivos.add(archivo);
    }
}