import static groovy.io.FileType.*

/**
processes file meta data to implement convention over configuration
**/
class MetaDataProcessor {

    static void fixMetaDataHeader(File sourceDir, Map config) {
        sourceDir.traverse(type: FILES) { file ->
            if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc|html|md)$') {
                if (file.name.startsWith("_") || file.name.startsWith(".")) {
                    //ignore
                } else {
                    def origText = file.text
                    def text = ""
                    def jbake = [
                        status: "published",
                        order: -1,
                        type: 'page_toc'
                    ]
                    if (file.name.toLowerCase() ==~ '^.*(md|html)$') {
                        jbake.type = 'page'
                    }
                    def beforeToc = ""
                    if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc)$') {
                        (text, beforeToc) = parseAsciiDocAttributes(origText, jbake)
                    } else {
                        text = parseOtherAttributes(origText, jbake)
                    }
                    def name = file.canonicalPath - (sourceDir.canonicalPath + File.separator)
                    if (File.separator == '\\') {
                        name = name.split("\\\\")
                    } else {
                        name = name.split("/")
                    }
                    if (name.size() > 1) {
                        if (!jbake.menu) {
                            jbake.menu = name[0]
                            if (jbake.menu ==~ /[0-9]+[-_].*/) {
                                jbake.menu = jbake.menu.split("[-_]", 2)[1]
                            }
                        }
                        def docname = name[-1]
                        if (docname ==~ /[0-9]+[-_].*/) {
                            jbake.order = docname.split("[-_]", 2)[0]
                            docname = docname.split("[-_]", 2)[1]
                        }
                        if (name.size() > 2) {
                            if ((jbake.order as Integer) == 0) {
                                def secondLevel = name[1]
                                if (secondLevel ==~ /[0-9]+[-_].*/) {
                                    jbake.order = secondLevel.split("[-_]", 2)[0]
                                }
                            } else {
                                if (((jbake.order ?: '1') as Integer) > 0) {
                                    //
                                } else {
                                    jbake.status = "draft"
                                }
                            }
                        }
                        if (jbake.order == -1 && docname.startsWith('index')) {
                            jbake.order = -987654321
                            jbake.status = "published"
                        }
                        if (jbake.order == -1 && jbake.type == 'post') {
                            jbake.order = 0
                            try {
                                jbake.order = Date.parse("yyyy-MM-dd", jbake.date).time / 100000
                            } catch (Exception e) {
                                logger.error "unparsable date ${jbake.date} in $name"
                            }
                            jbake.status = "published"
                        }
                        def leveloffset = 0
                        if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc)$') {
                            text.eachLine { line ->
                                if (!jbake.title && line ==~ "^=+ .*") {
                                    jbake.title = (line =~ "^=+ (.*)")[0][1]
                                    def level = (line =~ "^(=+) .*")[0][1]
                                    if (level == "=") {
                                        leveloffset = 1
                                    }
                                }
                            }
                        } else {
                            if (file.name.toLowerCase() ==~ '^.*(html)$') {
                                if (!jbake.title) {
                                    text.eachLine { line ->
                                        if (!jbake.title && line ==~ "^<h[1-9]>.*</h.*") {
                                            jbake.title = (line =~ "^<h[1-9]>(.*)</h.*")[0][1]
                                        }
                                    }
                                }
                            } else {
                                if (!jbake.title) {
                                    text.eachLine { line ->
                                        if (!jbake.title && line ==~ "^#+ .*") {
                                            jbake.title = (line =~ "^#+ (.*)")[0][1]
                                        }
                                    }
                                }
                            }
                        }
                        if (!jbake.title) {
                            jbake.title = docname
                        }
                        if (leveloffset == 1) {
                            text = text.replaceAll("(?ms)^(=+) ", '$1= ')
                        }
                        if (config.microsite.customConvention) {
                            def binding = new Binding([
                                file: file,
                             sourceDir: sourceDir,
                                config: config,
                                headers: jbake
                            ])
                            def shell = new GroovyShell(getClass().getClassLoader(), binding)
                            shell.evaluate(config.microsite.customConvention)
                            logger.debug jbake
                        }
                        def header = renderHeader(file.name, jbake)
                        if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc)$') {
                            file.write(header + "\nifndef::dtc-magic-toc[]\n:dtc-magic-toc:\n$beforeToc\n\n:toc: left\n\n++++\n<!-- endtoc -->\n++++\nendif::[]\n" + text, "utf-8")
                        } else {
                            file.write(header + "\n" + text, "utf-8")
                        }
                    }
                }
            }
        }
    }

    static String renderHeader(String fileName, Map jbake) {
        def header = ''
        if (fileName.toLowerCase() ==~ '^.*(html|md)$') {
            jbake.each { key, value ->
                if (key == 'order') {
                    header += "jbake-${key}=${(value ?: '1') as Integer}\n"
                } else {
                    if (key in ['type', 'status']) {
                        header += "${key}=${value}\n"
                    } else {
                        header += "jbake-${key}=${value}\n"
                    }
                }
            }
            header += "~~~~~~\n\n"
        } else {
            jbake.each { key, value ->
                if (key == 'order') {
                    header += ":jbake-${key}: ${(value ?: '1') as Integer}\n"
                } else {
                    header += ":jbake-${key}: ${value}\n"
                }
            }
        }
        return header
    }
    static List parseAsciiDocAttributes(String originalText, Map jbakeAttributes) {
        def parseAttribs = true
        def text = ""
        def beforeToc = ""
        originalText.eachLine { line ->
            if (parseAttribs && line.startsWith(":jbake")) {
                def parsedJbakeAttribute = (line - ":jbake-").split(": +", 2)
                if(parsedJbakeAttribute.length != 2) {
                    logger.warn("jbake-attribute is not valid or Asciidoc conform: $line")
                    logger.warn("jbake-attribute $line will be ignored, trying to continue...")
                } else {
                    jbakeAttributes[parsedJbakeAttribute[0]] = parsedJbakeAttribute[1]
                }
            } else {
                if (line.startsWith("[")) {
                    parseAttribs = false
                }
                text += line + "\n"
                if (line.startsWith(":toc") ) {
                    beforeToc += line + "\n"
                }
            }
        }
        return [text, beforeToc]
    }

    static String parseOtherAttributes(String originalText, Map jbakeAttributes) {
        if (originalText.contains('~~~~~~')) {
            def parseAttribs = true
            def text = ""
            originalText.eachLine { line ->
                if (parseAttribs && line.contains("=")) {
                    line = (line - "jbake-").split("=", 2)
                    jbakeAttributes[line[0]] = line[1]
                } else {
                    if (line.startsWith("~~~~~~")) {
                        parseAttribs = false
                    } else {
                        text += line + "\n"
                    }
                }
            }
            return text
        } else {
            return originalText
        }
    }


}
