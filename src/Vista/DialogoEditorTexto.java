package Vista;

import java.awt.*;
import java.io.File;
import javax.swing.*;

public class DialogoEditorTexto extends JDialog {

    private final JTextArea areaTexto;
    private final JButton btnGuardar;
    private boolean guardado = false;

    public DialogoEditorTexto(Frame padre, File archivo, String contenidoInicial) {
        super(padre, "Editor: " + archivo.getName(), true);
        setSize(650, 480);
        setLocationRelativeTo(padre);
        setLayout(new BorderLayout(5, 5));

        areaTexto = new JTextArea(contenidoInicial);
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(areaTexto), BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnGuardar = new JButton("Guardar Cambios");
        JButton btnCerrar = new JButton("Cerrar");

        btnGuardar.addActionListener(e -> {
            guardado = true;
            dispose();
        });
        btnCerrar.addActionListener(e -> dispose());

        panelSur.add(btnGuardar);
        panelSur.add(btnCerrar);
        add(panelSur, BorderLayout.SOUTH);
    }

    public boolean isGuardado() {
        return guardado;
    }

    public String getContenidoEditado() {
        return areaTexto.getText();
    }
}
