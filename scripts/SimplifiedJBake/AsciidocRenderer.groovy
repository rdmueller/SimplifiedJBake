import org.asciidoctor.Asciidoctor
import org.asciidoctor.OptionsBuilder
import org.asciidoctor.SafeMode

class AsciidocRenderer {

    static List renderAsciidocFiles(File baseDir, List<File> files, Map config) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create()
        Map attributes = [test: 'yo']
        def options = OptionsBuilder.options()
                .attributes(attributes)
                .toFile(false)
                .safe(SafeMode.UNSAFE)
                .get()
        def renderedPages = []
        files.each { File file ->
            File targetFile = new File(baseDir, file.path)
            def htmlContent = asciidoctor.convertFile(targetFile, options)
            def header = asciidoctor.readDocumentHeader(targetFile.text)

            def page = [
                    path      : file.path,
                    content   : [
                            body     : htmlContent,
                            file     : file.canonicalPath,
                            rootpath : '../' * (file.path.split('/').size() - 1),
                            title     : 'empty title',
                            newEntries: ['newEntry1', 'newEntry2'],
                            sourceui : file.path.replaceFirst(/[.]adoc$/, '.html'),
                            uri      : file.path.replaceFirst(/[.]adoc$/, '.html')
                    ],
                    sourceui : file.path.replaceFirst(/[.]adoc$/, '.html'),
                    uri      : file.path.replaceFirst(/[.]adoc$/, '.html')
                ] 
            def docAttributes = attributes + header.getAttributes() + parseAttributes(targetFile.text, /:(\S+):\s*(.+)/)
            docAttributes.each { key, value ->
                if (key.startsWith('jbake-')) {
                    def newKey = key - 'jbake-'
                    if (!page.containsKey(newKey)) {
                        page[key - 'jbake-'] = value
                        page.content[key - 'jbake-'] = value
                    }
                }
                page[key] = value
                page.content[key] = value
            }
            renderedPages << page
        }
        asciidoctor.shutdown()
        return renderedPages
    }

    static Map parseAttributes(String content, String pattern) {
        def matcher = content =~ pattern
        def attributes = [:]
        matcher.each { fullMatch, key, value ->
            attributes[key] = value
        }
        return attributes
    }

}