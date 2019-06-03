package coverstats.server.utils

import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory

@Suppress("UNCHECKED_CAST")
fun <T> JAXBContext.xmlSecureUnmarshall(input: String): T {
    val xif = XMLInputFactory.newFactory()
    xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    xif.setProperty(XMLInputFactory.SUPPORT_DTD, false)
    val xsr = xif.createXMLStreamReader(StringReader(input))
    return createUnmarshaller().unmarshal(xsr) as T
}