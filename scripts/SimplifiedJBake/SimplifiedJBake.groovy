@Grab(group='org.asciidoctor', module='asciidoctorj', version='2.4.3')

// Import der lokalen Klassen
import static ConfigLoader
import static FileManager
import static MetaDataProcessor
import static AsciidocRenderer
import static TemplateProcessor

import java.util.logging.Logger
import java.util.logging.Level

class SimplifiedJBake {

    static final Logger logger = Logger.getLogger("SimplifiedJBake")
    static final String root = ""
    static final String SOURCE_DIR = root+'./src/docs'
    static final String TARGET_DIR = root+'./newbuild2'
    // relative to TARGET_DIR
    static final String TARGET_PATH = root+'microsite/output'
    // relative to TARGET_DIR
    static final String TMP_FOLDER = root+'microsite/tmp/site/doc'
    // relative to SOURCE_DIR
    static final String ASSET_FOLDER = '../site/assets'
    static final List<String> SUPPORTED_EXTENSIONS = ['ad', 'adoc', 'asciidoc', 'html', 'md']

    def renderedPages = []
    def config = [:]
    def targetDir = new File(TARGET_DIR)
    def sourceDir = new File(SOURCE_DIR)

    static void main(String[] args) {
        new SimplifiedJBake().run(args)
    }

    def run(args) {

        // check arguments
        if (args.length == 0) {
            println "Please provide the directory path containing .adoc files"
            return
        }

        // load config
        this.config = ConfigLoader.loadConfig("docToolchainConfig.groovy")

        // configure and ensure folders
        File baseDir = new File(args[0])
        def buildDir = new File(targetDir, TARGET_PATH)
        buildDir.mkdirs()
        File tmpDir = new File(targetDir, TMP_FOLDER)
        tmpDir.mkdirs()
        File assetsDir = new File(baseDir, ASSET_FOLDER)

        // copy source files to temp folder
        FileManager.copyDir(baseDir, tmpDir)
        baseDir = tmpDir

        // copy assests to build folder
        FileManager.copyDir(assetsDir, buildDir)

        // convention over configuration
        MetaDataProcessor.fixMetaDataHeader(tmpDir, config)

        // read all source adoc files to array
        def adocFiles = FileManager.findAsciidocFiles(tmpDir)
        // log found adoc files with level debug
        logger.log(Level.FINE, "found adocFiles: $adocFiles")

        
        renderedPages = AsciidocRenderer.renderAsciidocFiles(tmpDir, adocFiles, config)

        writeHtmlFiles(renderedPages, buildDir, config)
    }

    def writeHtmlFiles(List renderedPages, File buildDir, Map config) {
    
        def sourceFolder = new File('src/site')
        def template_config = [sourceFolder: sourceFolder.canonicalPath]

        config.microsite.each { key, value ->
            template_config['site_' + key] = value
        }
        def binding = [
                'content'         : [:],
                'published_content': renderedPages,
                'config'          : template_config,
                'sourceFolder'    : sourceFolder,
                'version'         : "0.1",
        ]

        renderedPages.each { page ->
            def htmlFile = new File(buildDir, page.path.replaceFirst(/[.]adoc$/, '') + '.html')
            htmlFile.parentFile.mkdirs()
            logger.info (htmlFile.canonicalPath)
            binding.content = page.content
            htmlFile.text = TemplateProcessor.processWithTemplate(
                    page.attributes,
                    binding, 
                    renderedPages, 
                    config
                )
        }
    }

}