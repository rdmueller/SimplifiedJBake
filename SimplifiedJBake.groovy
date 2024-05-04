@Grab(group='org.asciidoctor', module='asciidoctorj', version='2.4.3')
import org.asciidoctor.Asciidoctor
import org.asciidoctor.OptionsBuilder
import groovy.text.SimpleTemplateEngine

// Placeholder for storing rendered pages
def renderedPages = []

// Entry point of the script
def main(args) {
    if (args.length == 0) {
        println "Please provide the directory path containing .adoc files"
        return
    }

    def adocFiles = findAsciidocFiles(args[0])

    // Render each Asciidoc file to HTML and store in the list
    renderAsciidocFiles(adocFiles)

    // Assuming a build directory in the current working directory
    def buildDir = new File('build')
    if (!buildDir.exists()) {
        buildDir.mkdirs()
    }

    // Write rendered HTML files to the build directory
    writeHtmlFiles(buildDir)
}

def findAsciidocFiles(String directoryPath) {
    def adocFiles = []
    def directory = new File(directoryPath)
    directory.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.adoc$/) { file ->
        adocFiles << file
    }
    return adocFiles
}

def renderAsciidocFiles(List<File> files) {
    Asciidoctor asciidoctor = Asciidoctor.Factory.create()
    files.each { File file ->
        def htmlContent = asciidoctor.convertFile(file, OptionsBuilder.options().toFile(false))
        renderedPages << [path: file.path, content: htmlContent] 
    }
}

def writeHtmlFiles(File buildDir) {
    renderedPages.each { page ->
        def htmlFile = new File(buildDir, page.path.replaceFirst(/.*\/|\.adoc$/, '') + '.html')
        htmlFile.text = processTemplate(page.content)
    }
}

def processTemplate(String content) {
    def engine = new SimpleTemplateEngine()
    def script = engine.createTemplate(content).make()
    return expandIncludes(script.toString())
}

def expandIncludes(String content) {
    def pattern = /<%\s*include\s*"(.+?)"\s*%>/
    while (content ==~ pattern) {
        content = content.replaceAll(pattern) { match ->
            def includePath = match[1]
            def file = new File(includePath)
            if (!file.exists()) throw new FileNotFoundException("Included file $includePath not found")
            file.text
        }
    }
    return content
}

main(args)
