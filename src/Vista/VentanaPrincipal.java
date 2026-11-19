package Vista;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class VentanaPrincipal extends JFrame {

    public JTextField txtRuta = new JTextField(30);
    public JButton btnSeleccionarRuta = new JButton("Examinar...");
    public JButton btnAnalizar = new JButton("Analizar y Cargar Árbol");
    public JButton btnCancelar = new JButton("Cancelar");

    public JTextField txtBusqueda = new JTextField(15);
    public JTextField txtExtension = new JTextField(6);
    public JTextField txtTamMin = new JTextField("0", 6);
    public JTextField txtTamMax = new JTextField("0", 6);
    public JTextField txtFechaMin = new JTextField(10);
    public JTextField txtFechaMax = new JTextField(10);
    public JTextField txtProfundidad = new JTextField("0", 4);
    public JButton btnBuscar = new JButton("Buscar");

    public JTree arbolDirectorios;
    public DefaultTreeModel modeloArbol;

    public JButton btnNuevaCarpeta = new JButton("Nueva Carpeta");
    public JButton btnNuevoArchivo = new JButton("Nuevo .txt");
    public JButton btnEditarTexto = new JButton("Abrir/Editar Texto");
    public JButton btnRenombrar = new JButton("Renombrar");
    public JButton btnCopiar = new JButton("Copiar");
    public JButton btnMover = new JButton("Mover");
    public JButton btnEliminar = new JButton("Eliminar");

    public JTable tablaBusqueda;
    public DefaultTableModel modeloTablaBusqueda;

    public JTextArea areaEstadisticas = new JTextArea();
    public JButton btnExportarTxt = new JButton("Exportar .TXT");
    public JButton btnExportarCsv = new JButton("Exportar .CSV");

    public JTable tablaDuplicados;
    public DefaultTableModel modeloTablaDuplicados;

    public JTextArea areaBitacora = new JTextArea();
    public JButton btnRefrescarBitacora = new JButton("Refrescar Bitácora");

    public JProgressBar barraProgreso = new JProgressBar();

    public VentanaPrincipal() {
        setTitle("Explorador, Analizador y Gestor de Archivos");
        setSize(1150, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel panelNorte = new JPanel(new GridLayout(2, 1, 4, 4));
        JPanel filaRuta = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaRuta.add(new JLabel("Directorio Raíz:"));
        filaRuta.add(txtRuta);
        filaRuta.add(btnSeleccionarRuta);
        filaRuta.add(new JLabel("Profundidad (0=ilimitada):"));
        filaRuta.add(txtProfundidad);
        filaRuta.add(btnAnalizar);
        btnCancelar.setEnabled(false);
        filaRuta.add(btnCancelar);

        JPanel filaFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaFiltros.add(new JLabel("Nombre:"));
        filaFiltros.add(txtBusqueda);
        filaFiltros.add(new JLabel("Ext:"));
        filaFiltros.add(txtExtension);
        filaFiltros.add(new JLabel("Min KB:"));
        filaFiltros.add(txtTamMin);
        filaFiltros.add(new JLabel("Max KB:"));
        filaFiltros.add(txtTamMax);
        filaFiltros.add(new JLabel("Desde (yyyy-MM-dd):"));
        filaFiltros.add(txtFechaMin);
        filaFiltros.add(new JLabel("Hasta:"));
        filaFiltros.add(txtFechaMax);
        filaFiltros.add(btnBuscar);

        panelNorte.add(filaRuta);
        panelNorte.add(filaFiltros);
        add(panelNorte, BorderLayout.NORTH);

        DefaultMutableTreeNode raizVacia = new DefaultMutableTreeNode("Sin directorio cargado");
        modeloArbol = new DefaultTreeModel(raizVacia);
        arbolDirectorios = new JTree(modeloArbol);

        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.add(new JScrollPane(arbolDirectorios), BorderLayout.CENTER);

        JPanel panelBotonesGestion = new JPanel(new GridLayout(4, 2, 4, 4));
        panelBotonesGestion.add(btnNuevaCarpeta);
        panelBotonesGestion.add(btnNuevoArchivo);
        panelBotonesGestion.add(btnEditarTexto);
        panelBotonesGestion.add(btnRenombrar);
        panelBotonesGestion.add(btnCopiar);
        panelBotonesGestion.add(btnMover);
        panelBotonesGestion.add(btnEliminar);
        panelIzquierdo.add(panelBotonesGestion, BorderLayout.SOUTH);

        JTabbedPane pestanas = new JTabbedPane();

        modeloTablaBusqueda = new DefaultTableModel(new String[]{"Nombre", "Ruta Absoluta", "Tamaño (Bytes)", "Fecha Modificación"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Long.class;
                }
                return String.class;
            }
        };
        tablaBusqueda = new JTable(modeloTablaBusqueda);
        tablaBusqueda.setAutoCreateRowSorter(true);
        pestanas.addTab("Resultados de Búsqueda", new JScrollPane(tablaBusqueda));

        JPanel panelStats = new JPanel(new BorderLayout());
        areaEstadisticas.setEditable(false);
        areaEstadisticas.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panelStats.add(new JScrollPane(areaEstadisticas), BorderLayout.CENTER);
        JPanel panelExport = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelExport.add(btnExportarTxt);
        panelExport.add(btnExportarCsv);
        panelStats.add(panelExport, BorderLayout.SOUTH);
        pestanas.addTab("Estadísticas", panelStats);

        modeloTablaDuplicados = new DefaultTableModel(new String[]{"Nombre Archivo", "Tamaño", "Ruta Completa"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaDuplicados = new JTable(modeloTablaDuplicados);
        pestanas.addTab("Duplicados", new JScrollPane(tablaDuplicados));

        JPanel panelBitacora = new JPanel(new BorderLayout());
        areaBitacora.setEditable(false);
        areaBitacora.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panelBitacora.add(new JScrollPane(areaBitacora), BorderLayout.CENTER);
        panelBitacora.add(btnRefrescarBitacora, BorderLayout.SOUTH);
        pestanas.addTab("Bitácora", panelBitacora);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, pestanas);
        splitPane.setDividerLocation(340);
        add(splitPane, BorderLayout.CENTER);

        barraProgreso.setStringPainted(true);
        barraProgreso.setString("Listo");
        add(barraProgreso, BorderLayout.SOUTH);
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarInfo(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirmarAccion(String mensaje) {
        int respuesta = JOptionPane.showConfirmDialog(this, mensaje, "Confirmar Operación",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return respuesta == JOptionPane.YES_OPTION;
    }
}
