package alp.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utilitaire pour télécharger automatiquement les instances ALP depuis OR-Library.
 */
public class InstanceDownloader {

    private static final String[] INSTANCE_URLS = {
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland1.txt",
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland2.txt",
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland3.txt", 
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland4.txt",
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland5.txt",
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland6.txt",
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland7.txt",
        "https://people.brunel.ac.uk/~mastjjb/jeb/orlib/files/airland8.txt"
    };
    
    /**
     * Télécharge toutes les instances dans le dossier spécifié.
     * 
     * @param targetDir Le répertoire cible pour les fichiers téléchargés
     * @return true si tous les téléchargements ont réussi, false sinon
     */
    public static boolean downloadAllInstances(String targetDir) {
        try {
            // Créer le répertoire cible s'il n'existe pas
            Files.createDirectories(Paths.get(targetDir));
            
            boolean allSuccessful = true;
            
            for (String url : INSTANCE_URLS) {
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                String targetPath = targetDir + File.separator + fileName;
                
                System.out.println("Téléchargement de " + fileName + "...");
                
                try {
                    downloadFile(url, targetPath);
                    System.out.println("  Téléchargement réussi: " + targetPath);
                } catch (IOException e) {
                    System.err.println("  Échec du téléchargement: " + e.getMessage());
                    allSuccessful = false;
                }
            }
            
            return allSuccessful;
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Télécharge un fichier à partir d'une URL.
     * 
     * @param urlStr URL du fichier à télécharger
     * @param destinationPath Chemin local où sauvegarder le fichier
     */
    private static void downloadFile(String urlStr, String destinationPath) throws IOException {
        URL url = new URL(urlStr);
        try (
            InputStream is = url.openStream();
            ReadableByteChannel rbc = Channels.newChannel(is);
            FileOutputStream fos = new FileOutputStream(destinationPath)
        ) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    
    /**
     * Programme principal pour exécuter le téléchargeur d'instances.
     */
    public static void main(String[] args) {
        String targetDir = args.length > 0 ? args[0] : "instances";
        System.out.println("Téléchargement des instances ALP dans: " + targetDir);
        
        boolean success = downloadAllInstances(targetDir);
        
        if (success) {
            System.out.println("Tous les téléchargements sont terminés avec succès!");
        } else {
            System.out.println("Certains téléchargements ont échoué. Vérifiez les messages d'erreur ci-dessus.");
        }
    }
}
