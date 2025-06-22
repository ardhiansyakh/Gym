package Main;
import View.login;
import javax.swing.SwingUtilities;

/**
 *
 * @author Ardhiansyakh
 */
public class Main {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new login().setVisible(true);
        });
    }
}
 