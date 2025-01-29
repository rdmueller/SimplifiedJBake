import java.nio.file.*
import static groovy.io.FileType.*
import java.util.logging.Level

class FileManager {

    static void copyDir(File sourceDir, File destDir) {

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            SimplifiedJBake.logger.log(Level.SEVERE, "Source directory does not exist or is not a directory: ${sourceDir.path}")
            return
        }

        sourceDir.traverse { file ->
            def relativePath = sourceDir.toPath().relativize(file.toPath())
            def targetFile = new File(destDir, relativePath.toString())
            if (file.isDirectory()) {
                targetFile.mkdirs()
            } else {
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
    static List<File> findAsciidocFiles(File baseDir) {
        def adocFiles = []
        def directory = baseDir
        directory.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.adoc$/) { file ->
            adocFiles << baseDir.toPath().relativize(file.toPath()).toFile()
        }
        return adocFiles
    }


}