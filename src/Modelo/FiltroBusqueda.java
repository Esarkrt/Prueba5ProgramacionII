package Modelo;

import Excepciones.RutaInvalidaException;

public class FiltroBusqueda {

    private final String texto;
    private final String extension;
    private final long tamanoMinBytes;
    private final long tamanoMaxBytes;
    private final long fechaMinMillis;
    private final long fechaMaxMillis;
    private final int profundidadMax;

    public FiltroBusqueda(String texto, String extension, long tamanoMinKB, long tamanoMaxKB,
            long fechaMinMillis, long fechaMaxMillis, int profundidadMax) throws RutaInvalidaException {
        if (tamanoMinKB < 0 || tamanoMaxKB < 0) {
            throw new RutaInvalidaException("Los tamaños mínimo y máximo no pueden ser negativos.");
        }
        if (tamanoMaxKB > 0 && tamanoMinKB > tamanoMaxKB) {
            throw new RutaInvalidaException("El tamaño mínimo no puede ser mayor al tamaño máximo.");
        }
        if (fechaMinMillis > 0 && fechaMaxMillis > 0 && fechaMinMillis > fechaMaxMillis) {
            throw new RutaInvalidaException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (profundidadMax < 0) {
            throw new RutaInvalidaException("La profundidad máxima no puede ser negativa.");
        }

        if (texto != null) {
            this.texto = texto.trim().toLowerCase();
        } else {
            this.texto = "";
        }

        if (extension != null) {
            this.extension = extension.trim().toLowerCase().replace(".", "");
        } else {
            this.extension = "";
        }

        this.tamanoMinBytes = tamanoMinKB * 1024L;

        if (tamanoMaxKB > 0) {
            this.tamanoMaxBytes = tamanoMaxKB * 1024L;
        } else {
            this.tamanoMaxBytes = -1L;
        }

        this.fechaMinMillis = fechaMinMillis;
        this.fechaMaxMillis = fechaMaxMillis;
        this.profundidadMax = profundidadMax;
    }

    public String getTexto() {
        return texto;
    }

    public String getExtension() {
        return extension;
    }

    public long getTamanoMinBytes() {
        return tamanoMinBytes;
    }

    public long getTamanoMaxBytes() {
        return tamanoMaxBytes;
    }

    public long getFechaMinMillis() {
        return fechaMinMillis;
    }

    public long getFechaMaxMillis() {
        return fechaMaxMillis;
    }

    public int getProfundidadMax() {
        return profundidadMax;
    }
}
