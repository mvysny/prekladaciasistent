package fi.mavi

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import java.io.File

private val options = Options().apply {
    addOption("s", true, """The Lucene search query, e.g. 'mandate~' or '"happy tree friends"'. See https://lucene.apache.org/core/2_9_4/queryparsersyntax.html for more info.""")
    addOption("h", "Shows help")
    addOption("i", true, "Indexes given CSV file")
}

fun main(args: Array<String>) {
    val cmdline = DefaultParser().parse(options, args)
    when {
        cmdline.hasOption('h') -> showHelp()
        cmdline.hasOption('s') -> searchFor(cmdline.getOptionValue('s'))
        cmdline.hasOption('i') -> index(File(cmdline.getOptionValue('i')))
        else -> showHelp()
    }
}

fun showHelp() {
    HelpFormatter().printHelp("pa [-h] [-s 'search query'] [-i file.csv]", options)
}

private val luceneDir: File = File("lucene_index").absoluteFile

fun useLucene(block: (FSDirectory)->Unit) {
    FSDirectory.open(luceneDir).use(block)
}

fun searchFor(luceneQuery: String) {
    val parser = QueryParser(Version.LUCENE_30, "index", StandardAnalyzer(Version.LUCENE_30))
    val parsedQuery = parser.parse(luceneQuery)
    useLucene { lucene ->
        IndexReader.open(lucene).use { indexReader: IndexReader ->
            IndexSearcher(indexReader).use { searcher ->
                val docs: TopDocs = searcher.search(parsedQuery, 100)
                docs.scoreDocs.asSequence().map { searcher.doc(it.doc) } .forEach { doc ->
                    println(doc.get("row"))
                }
            }
        }
    }
}

fun index(csvFile: File) {
    csvFile.deleteRecursively()
    require(csvFile.exists()) { "$csvFile does not exist" }
    println("Indexing $csvFile")
    useLucene { lucene ->
        StandardAnalyzer(Version.LUCENE_30).use { analyzer ->
            IndexWriter(lucene, IndexWriterConfig(Version.LUCENE_30, analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE)).use { luceneWriter ->
                CSVParser.parse(csvFile, Charsets.UTF_8, CSVFormat.DEFAULT).use { parser ->
                    parser.forEach { record: CSVRecord ->
                        val doc = Document()
                        doc.add(Field("index", record.joinToString(" "), Field.Store.NO, Field.Index.ANALYZED))
                        doc.add(Field("row", record.joinToString(" | "), Field.Store.YES, Field.Index.NO))
                        luceneWriter.addDocument(doc)
                    }
                }
                luceneWriter.forceMerge(1, true)  // optimize Lucene Index
            }
        }
    }
}
