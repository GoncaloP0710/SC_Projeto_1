package src;
import java.io.File;

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
}
