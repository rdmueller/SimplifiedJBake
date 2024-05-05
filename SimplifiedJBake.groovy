@Grab(group='org.asciidoctor', module='asciidoctorj', version='2.4.3')
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Attributes
import org.asciidoctor.OptionsBuilder
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic

// Placeholder for storing rendered pages
def renderedPages = []

// Entry point of the script
def run(args) {
    if (args.length == 0) {
        println "Please provide the directory path containing .adoc files"
        return
    }

    // Assuming a build directory in the current working directory
    def buildDir = new File('build')
    if (!buildDir.exists()) {
        buildDir.mkdirs()
    }

    def adocFiles = findAsciidocFiles(args[0])
    println adocFiles
    // Render each Asciidoc file to HTML and store in the list
    renderedPages = renderAsciidocFiles(adocFiles)

    // Write rendered HTML files to the build directory
    writeHtmlFiles(buildDir)
}

def findAsciidocFiles(String directoryPath) {
    def adocFiles = []
    def directory = new File(directoryPath)
    println directory.canonicalPath
    directory.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.adoc$/) { file ->
        adocFiles << file
    }
    return adocFiles
}

def renderAsciidocFiles(List<File> files) {
    def asciidoctor = Asciidoctor.Factory.create()
    def options = OptionsBuilder.options()
                                .attributes([test:'yo'])
                                .toFile(false)
                                .get()
    renderedPages = []
    println "Convert AsciiDoc"
    files.each { File file ->
        println file.canonicalPath
        def htmlContent = asciidoctor.convertFile(file, options)
        def docAttributes = parseAttributes(file.text)
        renderedPages << [path: file.path, content: htmlContent, attributes: docAttributes] 
    }
    asciidoctor.shutdown()
    return renderedPages
}

def writeHtmlFiles(File buildDir) {
    renderedPages.each { page ->
        def htmlFile = new File(buildDir, page.path.replaceFirst(/.*\/|\.adoc$/, '') + '.html')
        htmlFile.text = processWithTemplate(page.content, renderedPages)
    }
}

def processWithTemplate(String content, List renderedPages) {
    // Path to the template file
    def templatePath = 'site/templates/page.gsp'
    def templateContent = new File(templatePath).text
    templateContent = expandIncludes(templateContent)

    def engine = new SimpleTemplateEngine()
    def binding = ['content': content, 'renderedPages': renderedPages]
    def template = engine.createTemplate(templateContent).make(binding)

    return template.toString()
}

def expandIncludes(String content) {

    def pattern = /<%\s*include\s*\"(.+?)\"\s*%>/
    def matcher = content =~ pattern
    def baseDir = new File('site/templates')

    matcher.each { fullMatch, fileName ->
        File includeFile = new File(baseDir, fileName)
        if (!includeFile.exists()) {
            println "Include-File $fileName not found."
        } else {
            String fileContent = includeFile.text
            content = content.replace(fullMatch, fileContent)
        }
    }
    return content
}
def parseAttributes(String content) {
    def pattern = /:(\S+):\s*(.+)/
    def matcher = content =~ pattern
    def attributes = [:]
    matcher.each { fullMatch, key, value ->
        attributes[key] = value
    }
    return attributes
}
run(args)
