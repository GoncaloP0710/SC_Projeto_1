package server;
import java.io.File;

/**
 * @author André Reis fc58192
 * @author Gonçalo Pinto fc58178
 * @author José Brás fc55449
 */
public class UtilsIoT {
    
    private UtilsIoT() {
    }

    /**
     * Verifica se existe um ficheiro com o caminho dado
     * 
     * @param path caminho para ficheiro
     * @return true se existe um ficheiro com o caminho dado
     */
    public static boolean dataExist(String path) {
        File f = new File(path);
        return (f.exists() && !f.isDirectory());
    }

    /**
     * Verifica se a string passada representa um Integer
     * 
     * @param input string a ser verificada
     * @return true se a string passada representa um Integer
     */
    public static boolean isInteger( String input ) {
        try {
            Integer.valueOf( input );
            return true;
        }
        catch( NumberFormatException e ) {
            return false;
        }
    }
}
