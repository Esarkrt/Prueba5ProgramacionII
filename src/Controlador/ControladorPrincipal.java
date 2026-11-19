package Controlador;

import Excepciones.RutaInvalidaException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import Modelo.*;
import Vista.*;

public class ControladorPrincipal {

    private final VentanaPrincipal vista;
    private final PersistenciaManager persistencia;
    private final AnalizadorRecursivo analizador;
    private final GestorArchivos gestor;
    private EstadisticasDirectorio ultimasEstadisticas;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public ControladorPrincipal(VentanaPrincipal vista) {
        this.vista = vista;
        this.persistencia = new PersistenciaManager();
        this.analizador = new AnalizadorRecursivo(persistencia);
        this.gestor = new GestorArchivos(persistencia);
        this.sdf.setLenient(false);

        cargarConfiguracionInicial();
        registrarEventos();
        actualizarVistaBitacora();
    }

    private void cargarConfiguracionInicial() {
        Map<String, String> config = persistencia.cargarConfiguracion();
        vista.txtRuta.setText(config.getOrDefault("ultimaRuta", ""));
        vista.txtBusqueda.setText(config.getOrDefault("ultimoTexto", ""));
        vista.txtExtension.setText(config.getOrDefault("ultimaExt", ""));
        vista.txtTamMin.setText(config.getOrDefault("tamMin", "0"));
        vista.txtTamMax.setText(config.getOrDefault("tamMax", "0"));
        vista.txtProfundidad.setText(config.getOrDefault("profundidad", "0"));
    }

    private void guardarConfiguracionActual() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("ultimaRuta", vista.txtRuta.getText().trim());
            config.put("ultimoTexto", vista.txtBusqueda.getText().trim());
            config.put("ultimaExt", vista.txtExtension.getText().trim());
            config.put("tamMin", vista.txtTamMin.getText().trim());
            config.put("tamMax", vista.txtTamMax.getText().trim());
            config.put("profundidad", vista.txtProfundidad.getText().trim());
            persistencia.guardarConfiguracion(config);
        } catch (IOException ignored) {
        }
    }

    private void registrarEventos() {
        vista.btnSeleccionarRuta.addActionListener(e -> seleccionarDirectorio());
        vista.btnAnalizar.addActionListener(e -> ejecutarAnalisisCompleto());
        vista.btnBuscar.addActionListener(e -> ejecutarBusqueda());
        vista.btnNuevaCarpeta.addActionListener(e -> crearCarpeta());
        vista.btnNuevoArchivo.addActionListener(e -> crearArchivoTexto());
        vista.btnEditarTexto.addActionListener(e -> abrirEditorTexto());
        vista.btnRenombrar.addActionListener(e -> renombrarElemento());
        vista.btnCopiar.addActionListener(e -> copiarArchivo());
        vista.btnMover.addActionListener(e -> moverArchivo());
        vista.btnEliminar.addActionListener(e -> eliminarElemento());
        vista.btnExportarTxt.addActionListener(e -> exportarReporte(false));
        vista.btnExportarCsv.addActionListener(e -> exportarReporte(true));
        vista.btnRefrescarBitacora.addActionListener(e -> actualizarVistaBitacora());
    }

    private void seleccionarDirectorio() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(vista) == JFileChooser.APPROVE_OPTION) {
            vista.txtRuta.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void ejecutarAnalisisCompleto() {
        try {
            File raiz = analizador.validarDirectorio(vista.txtRuta.getText());
            int profMax = Integer.parseInt(vista.txtProfundidad.getText().trim());
            if (profMax < 0) {
                throw new RutaInvalidaException("La profundidad no puede ser negativa.");
            }

            guardarConfiguracionActual();
            vista.btnAnalizar.setEnabled(false);
            vista.btnCancelar.setEnabled(true);
            vista.barraProgreso.setIndeterminate(true);
            vista.barraProgreso.setString("Analizando directorio...");

            SwingWorker<EstadisticasDirectorio, Void> worker = new SwingWorker<EstadisticasDirectorio, Void>() {
                DefaultMutableTreeNode nodoRaizArbol;

                @Override
                protected EstadisticasDirectorio doInBackground() {
                    nodoRaizArbol = construirNodoArbolRecursivo(raiz, 0, profMax, new ArrayList<>());
                    return analizador.analizar(raiz, profMax);
                }

                @Override
                protected void done() {
                    vista.btnAnalizar.setEnabled(true);
                    vista.btnCancelar.setEnabled(false);
                    vista.barraProgreso.setIndeterminate(false);
                    vista.barraProgreso.setString("Proceso finalizado");

                    try {
                        if (!isCancelled()) {
                            ultimasEstadisticas = get();
                            vista.modeloArbol.setRoot(nodoRaizArbol);
                            mostrarEstadisticasEnVista(ultimasEstadisticas);
                            mostrarDuplicadosEnVista(ultimasEstadisticas.getDuplicados());
                            persistencia.registrar("ANALIZAR", raiz.getAbsolutePath());
                            actualizarVistaBitacora();
                        }
                    } catch (Exception ex) {
                        vista.mostrarError("Error durante el recorrido: " + ex.getMessage());
                    }
                }
            };

            vista.btnCancelar.addActionListener(ev -> {
                analizador.cancelar();
                worker.cancel(true);
                vista.barraProgreso.setString("Operación cancelada");
            });

            worker.execute();
        } catch (NumberFormatException ex) {
            vista.mostrarError("La profundidad debe ser un número entero.");
        } catch (RutaInvalidaException ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private DefaultMutableTreeNode construirNodoArbolRecursivo(File actual, int nivel, int profMax, List<String> visitados) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(new NodoArchivo(actual, nivel == 0));
        if (profMax > 0 && nivel >= profMax) {
            return nodo;
        }

        try {
            String canonica = actual.getCanonicalPath();
            if (visitados.contains(canonica)) {
                return nodo;
            }
            visitados.add(canonica);
        } catch (IOException e) {
            return nodo;
        }

        File[] hijos = actual.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                if (hijo.isDirectory()) {
                    nodo.add(construirNodoArbolRecursivo(hijo, nivel + 1, profMax, visitados));
                } else {
                    nodo.add(new DefaultMutableTreeNode(new NodoArchivo(hijo, false)));
                }
            }
        }
        return nodo;
    }

    private void ejecutarBusqueda() {
        try {
            File raiz = analizador.validarDirectorio(vista.txtRuta.getText());
            long minKB = Long.parseLong(vista.txtTamMin.getText().trim());
            long maxKB = Long.parseLong(vista.txtTamMax.getText().trim());
            int profMax = Integer.parseInt(vista.txtProfundidad.getText().trim());
            long fechaMin = parsearFecha(vista.txtFechaMin.getText().trim());
            long fechaMax = parsearFecha(vista.txtFechaMax.getText().trim());

            FiltroBusqueda filtro = new FiltroBusqueda(
                    vista.txtBusqueda.getText(), vista.txtExtension.getText(),
                    minKB, maxKB, fechaMin, fechaMax, profMax
            );

            guardarConfiguracionActual();
            vista.barraProgreso.setIndeterminate(true);
            vista.barraProgreso.setString("Buscando archivos...");

            SwingWorker<List<ArchivoInfo>, Void> worker = new SwingWorker<List<ArchivoInfo>, Void>() {
                @Override
                protected List<ArchivoInfo> doInBackground() {
                    return analizador.buscar(raiz, filtro);
                }

                @Override
                protected void done() {
                    vista.barraProgreso.setIndeterminate(false);
                    vista.barraProgreso.setString("Búsqueda completada");
                    try {
                        List<ArchivoInfo> encontrados = get();
                        vista.modeloTablaBusqueda.setRowCount(0);
                        if (encontrados.isEmpty()) {
                            vista.mostrarInfo("No se encontraron archivos que coincidan con los criterios.");
                        } else {
                            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            for (ArchivoInfo info : encontrados) {
                                vista.modeloTablaBusqueda.addRow(new Object[]{
                                    info.getNombre(),
                                    info.getRutaAbsoluta(),
                                    info.getTamanoBytes(),
                                    fmt.format(new Date(info.getFechaModificacion()))
                                });
                            }
                        }
                        actualizarVistaBitacora();
                    } catch (Exception ex) {
                        vista.mostrarError("Error en búsqueda: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        } catch (NumberFormatException ex) {
            vista.mostrarError("Los campos numéricos deben contener valores enteros válidos.");
        } catch (ParseException ex) {
            vista.mostrarError("Formato de fecha inválido. Use yyyy-MM-dd.");
        } catch (RutaInvalidaException ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private long parsearFecha(String textoFecha) throws ParseException {
        if (textoFecha.isEmpty()) {
            return 0L;
        }
        return sdf.parse(textoFecha).getTime();
    }

    private File obtenerArchivoSeleccionadoArbol() throws RutaInvalidaException {
        DefaultMutableTreeNode seleccionado = (DefaultMutableTreeNode) vista.arbolDirectorios.getLastSelectedPathComponent();
        if (seleccionado == null || !(seleccionado.getUserObject() instanceof NodoArchivo)) {
            throw new RutaInvalidaException("Seleccione primero un elemento en el árbol.");
        }
        return ((NodoArchivo) seleccionado.getUserObject()).getArchivo();
    }

    private void crearCarpeta() {
        try {
            File seleccionado = obtenerArchivoSeleccionadoArbol();
            File padre;
            if (seleccionado.isDirectory()) {
                padre = seleccionado;
            } else {
                padre = seleccionado.getParentFile();
            }
            String nombre = JOptionPane.showInputDialog(vista, "Nombre de la nueva carpeta (puede incluir subcarpetas ej. a/b):");
            if (nombre != null) {
                gestor.crearCarpeta(padre, nombre);
                ejecutarAnalisisCompleto();
            }
        } catch (RutaInvalidaException ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void crearArchivoTexto() {
        try {
            File seleccionado = obtenerArchivoSeleccionadoArbol();
            File padre;
            if (seleccionado.isDirectory()) {
                padre = seleccionado;
            } else {
                padre = seleccionado.getParentFile();
            }
            String nombre = JOptionPane.showInputDialog(vista, "Nombre del archivo (ej. notas.txt):");
            if (nombre != null) {
                String contenido = JOptionPane.showInputDialog(vista, "Contenido inicial del archivo:");
                gestor.crearArchivoTexto(padre, nombre, contenido);
                ejecutarAnalisisCompleto();
            }
        } catch (Exception ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void abrirEditorTexto() {
        try {
            File archivo = obtenerArchivoSeleccionadoArbol();
            if (!archivo.isFile()) {
                throw new RutaInvalidaException("Debe seleccionar un archivo, no una carpeta.");
            }
            if (archivo.length() > 2 * 1024 * 1024) {
                if (!vista.confirmarAccion("El archivo supera los 2 MB y puede ralentizar el editor. ¿Desea abrirlo de todas formas?")) {
                    return;
                }
            }
            if (!gestor.esArchivoTextoValido(archivo)) {
                if (!vista.confirmarAccion("El archivo parece ser binario y no texto plano. ¿Desea abrirlo de todas formas?")) {
                    return;
                }
            }

            String texto = gestor.leerTextoConFileReader(archivo);
            DialogoEditorTexto dialogo = new DialogoEditorTexto(vista, archivo, texto);
            dialogo.setVisible(true);

            if (dialogo.isGuardado()) {
                if (vista.confirmarAccion("¿Desea sobrescribir el archivo con los cambios realizados?")) {
                    gestor.guardarTextoConFileWriter(archivo, dialogo.getContenidoEditado());
                    actualizarVistaBitacora();
                    vista.mostrarInfo("Archivo guardado correctamente.");
                }
            }
        } catch (Exception ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void renombrarElemento() {
        try {
            File actual = obtenerArchivoSeleccionadoArbol();
            String nuevoNombre = JOptionPane.showInputDialog(vista, "Nuevo nombre:", actual.getName());
            if (nuevoNombre != null) {
                gestor.renombrar(actual, nuevoNombre);
                ejecutarAnalisisCompleto();
            }
        } catch (RutaInvalidaException ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void copiarArchivo() {
        try {
            File origen = obtenerArchivoSeleccionadoArbol();
            if (!origen.isFile()) {
                throw new RutaInvalidaException("Solo se pueden copiar archivos.");
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(vista) == JFileChooser.APPROVE_OPTION) {
                File destino = new File(chooser.getSelectedFile(), origen.getName());
                if (destino.exists() && !vista.confirmarAccion("El archivo ya existe en el destino. ¿Desea sobrescribirlo?")) {
                    return;
                }
                gestor.copiarArchivo(origen, destino);
                ejecutarAnalisisCompleto();
            }
        } catch (Exception ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void moverArchivo() {
        try {
            File origen = obtenerArchivoSeleccionadoArbol();
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(vista) == JFileChooser.APPROVE_OPTION) {
                File destino = new File(chooser.getSelectedFile(), origen.getName());
                if (destino.exists() && !vista.confirmarAccion("El elemento ya existe en el destino. ¿Desea sobrescribirlo?")) {
                    return;
                }
                gestor.moverArchivo(origen, destino);
                ejecutarAnalisisCompleto();
            }
        } catch (Exception ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void eliminarElemento() {
        try {
            File objetivo = obtenerArchivoSeleccionadoArbol();
            if (vista.confirmarAccion("¿Está seguro de eliminar permanentemente: " + objetivo.getName() + "?")) {
                gestor.eliminarRecursivo(objetivo);
                ejecutarAnalisisCompleto();
            }
        } catch (Exception ex) {
            vista.mostrarError(ex.getMessage());
        }
    }

    private void mostrarEstadisticasEnVista(EstadisticasDirectorio s) {
        StringBuilder sb = new StringBuilder();
        sb.append("ESTADÍSTICAS POR EXTENSIÓN\n");
        sb.append("-------------------------------------------------------------\n");
        sb.append(String.format(".txt  : %5d archivos | %12s | %6.2f%%%n", s.getCantTxt(), EstadisticasDirectorio.formatearTamano(s.getBytesTxt()), s.calcularPorcentaje(s.getCantTxt())));
        sb.append(String.format(".java : %5d archivos | %12s | %6.2f%%%n", s.getCantJava(), EstadisticasDirectorio.formatearTamano(s.getBytesJava()), s.calcularPorcentaje(s.getCantJava())));
        sb.append(String.format(".pdf  : %5d archivos | %12s | %6.2f%%%n", s.getCantPdf(), EstadisticasDirectorio.formatearTamano(s.getBytesPdf()), s.calcularPorcentaje(s.getCantPdf())));
        sb.append(String.format("Otros : %5d archivos | %12s | %6.2f%%%n", s.getCantOtros(), EstadisticasDirectorio.formatearTamano(s.getBytesOtros()), s.calcularPorcentaje(s.getCantOtros())));
        sb.append("-------------------------------------------------------------\n");
        sb.append(String.format("TOTAL : %5d archivos | %12s%n%n", s.getTotalArchivos(), EstadisticasDirectorio.formatearTamano(s.getTotalBytes())));

        sb.append("ESTADÍSTICAS GLOBALES\n");
        sb.append("-------------------------------------------------------------\n");
        sb.append("Carpetas totales      : ").append(s.getTotalCarpetas()).append("\n");
        sb.append("Carpetas vacías       : ").append(s.getCarpetasVacias()).append("\n");
        sb.append("Profundidad máxima   : ").append(s.getProfundidadMaxima()).append("\n");

        String rutaGrande = "N/A";
        if (s.getArchivoMasGrande() != null) {
            rutaGrande = s.getArchivoMasGrande().getAbsolutePath();
        }
        sb.append("Archivo más grande    : ").append(rutaGrande).append("\n");

        String rutaReciente = "N/A";
        if (s.getArchivoMasReciente() != null) {
            rutaReciente = s.getArchivoMasReciente().getAbsolutePath();
        }
        sb.append("Archivo más reciente  : ").append(rutaReciente).append("\n");

        vista.areaEstadisticas.setText(sb.toString());
    }

    private void mostrarDuplicadosEnVista(Map<String, List<File>> duplicados) {
        vista.modeloTablaDuplicados.setRowCount(0);
        for (List<File> listaArchivos : duplicados.values()) {
            for (File f : listaArchivos) {
                vista.modeloTablaDuplicados.addRow(new Object[]{
                    f.getName(),
                    EstadisticasDirectorio.formatearTamano(f.length()),
                    f.getAbsolutePath()
                });
            }
        }
    }

    private void exportarReporte(boolean esCsv) {
        if (ultimasEstadisticas == null) {
            vista.mostrarError("Primero debe analizar un directorio.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        String nombrePorDefecto;
        if (esCsv) {
            nombrePorDefecto = "reporte_estadisticas.csv";
        } else {
            nombrePorDefecto = "reporte_estadisticas.txt";
        }
        chooser.setSelectedFile(new File(nombrePorDefecto));

        if (chooser.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            File destino = chooser.getSelectedFile();
            if (destino.exists() && !vista.confirmarAccion("El archivo ya existe. ¿Desea sobrescribirlo?")) {
                return;
            }
            try {
                persistencia.exportarReporte(destino, ultimasEstadisticas, vista.txtRuta.getText(), esCsv);
                actualizarVistaBitacora();
                vista.mostrarInfo("Reporte exportado exitosamente.");
            } catch (IOException ex) {
                vista.mostrarError("Error al exportar reporte: " + ex.getMessage());
            }
        }
    }

    private void actualizarVistaBitacora() {
        try {
            vista.areaBitacora.setText(persistencia.leerBitacoraCompleta());
        } catch (IOException ex) {
            vista.areaBitacora.setText("No se pudo leer la bitácora.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal vista = new VentanaPrincipal();
            new ControladorPrincipal(vista);
            vista.setVisible(true);
        });
    }
}
