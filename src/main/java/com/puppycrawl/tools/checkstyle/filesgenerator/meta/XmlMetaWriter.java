package com.puppycrawl.tools.checkstyle.filesgenerator.meta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.puppycrawl.tools.checkstyle.meta.ModuleDetails;
import com.puppycrawl.tools.checkstyle.meta.ModulePropertyDetails;
import com.puppycrawl.tools.checkstyle.meta.ModuleType;

/**
 * Class to write module details object into an XML file.
 */
public final class XmlMetaWriter {

    /** Package containing Checkstyle's metadata resources. */
    private static final Path CHECKSTYLE_METADATA_PATH = Path.of(
            "com", "puppycrawl", "tools", "checkstyle", "meta");

    /** Checkstyle's base package. */
    private static final String CHECKSTYLE_PACKAGE = "com.puppycrawl.tools.checkstyle.";

    /** Name tag of metadata XML files. */
    private static final String XML_TAG_NAME = "name";

    /** Description tag of metadata XML files. */
    private static final String XML_TAG_DESCRIPTION = "description";

    /**
     * Do no allow {@code XmlMetaWriter} instances to be created.
     */
    private XmlMetaWriter() {
    }

    /**
     * Helper function to write module details to XML file.
     *
     * @param moduleDetails module details
     * @param checkstylePath path to the checkstyle source code directory
     * @throws TransformerException if a transformer exception occurs
     * @throws ParserConfigurationException if a parser configuration exception occurs
     * @throws IOException if an output directory cannot be created
     */
    public static void write(ModuleDetails moduleDetails, Path checkstylePath)
            throws TransformerException, ParserConfigurationException, IOException {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.newDocument();

        final Element rootElement = doc.createElement("checkstyle-metadata");
        final Element rootChild = doc.createElement("module");
        rootElement.appendChild(rootChild);

        doc.appendChild(rootElement);

        final Element checkModule = doc.createElement(moduleDetails.getModuleType().getLabel());
        rootChild.appendChild(checkModule);

        checkModule.setAttribute(XML_TAG_NAME, moduleDetails.getName());
        checkModule.setAttribute("fully-qualified-name",
                moduleDetails.getFullQualifiedName());
        checkModule.setAttribute("parent", moduleDetails.getParent());

        final Element desc = doc.createElement(XML_TAG_DESCRIPTION);
        final Node cdataDesc = doc.createCDATASection(moduleDetails.getDescription());
        desc.appendChild(cdataDesc);
        checkModule.appendChild(desc);
        createPropertySection(moduleDetails, checkModule, doc);
        final List<String> violationMessageKeys = moduleDetails.getViolationMessageKeys();
        if (!violationMessageKeys.isEmpty()) {
            final Element messageKeys = doc.createElement("message-keys");
            for (String msg : violationMessageKeys) {
                final Element messageKey = doc.createElement("message-key");
                messageKey.setAttribute("key", msg);
                messageKeys.appendChild(messageKey);
            }
            checkModule.appendChild(messageKeys);
        }

        final Path relativeOutputPath = getRelativeOutputPath(moduleDetails);
        writeToFile(doc, checkstylePath.resolve("src/main/resources")
                .resolve(relativeOutputPath));
        writeToFile(doc, checkstylePath.resolve("target/classes")
                .resolve(relativeOutputPath));
    }

    /**
     * Create the property section of the module detail object.
     *
     * @param moduleDetails module details
     * @param checkModule root doc element
     * @param doc document object
     */
    private static void createPropertySection(ModuleDetails moduleDetails, Element checkModule,
                                              Document doc) {
        final List<ModulePropertyDetails> moduleProperties = moduleDetails.getProperties();
        if (!moduleProperties.isEmpty()) {
            final Element properties = doc.createElement("properties");
            checkModule.appendChild(properties);
            for (ModulePropertyDetails modulePropertyDetails : moduleProperties) {
                final Element property = doc.createElement("property");
                properties.appendChild(property);
                property.setAttribute(XML_TAG_NAME, modulePropertyDetails.getName());
                property.setAttribute("type", modulePropertyDetails.getType());
                final String defaultValue = modulePropertyDetails.getDefaultValue();
                if (defaultValue != null && !"null".equals(defaultValue)) {
                    property.setAttribute("default-value", defaultValue);
                }
                final String validationType = modulePropertyDetails.getValidationType();
                if (validationType != null) {
                    property.setAttribute("validation-type", validationType);
                }
                final Element propertyDesc = doc.createElement(XML_TAG_DESCRIPTION);
                propertyDesc.appendChild(doc.createCDATASection(
                        modulePropertyDetails.getDescription()));
                property.appendChild(propertyDesc);
            }
        }
    }

    /**
     * Function to write the prepared document object into an XML file.
     *
     * @param document document updated with all module metadata
     * @param outputFile destination file
     * @throws TransformerException if a transformer exception occurs
     * @throws IOException if the output directory cannot be created
     */
    private static void writeToFile(Document document, Path outputFile)
            throws TransformerException, IOException {
        Files.createDirectories(outputFile.getParent());

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        final DOMSource source = new DOMSource(document);
        final StreamResult result = new StreamResult(outputFile.toFile());
        transformer.transform(source, result);
    }

    /**
     * Resolve a module's metadata path relative to a resource root.
     *
     * @param moduleDetails corresponding module details
     * @return metadata resource path
     */
    private static Path getRelativeOutputPath(ModuleDetails moduleDetails) {
        final Path result;
        final String fullQualifiedName = moduleDetails.getFullQualifiedName();
        if (fullQualifiedName.startsWith(CHECKSTYLE_PACKAGE)) {
            final String relativeClassName = fullQualifiedName
                    .substring(CHECKSTYLE_PACKAGE.length())
                    .replace('.', '/');
            result = CHECKSTYLE_METADATA_PATH.resolve(relativeClassName + ".xml");
        }
        else {
            String moduleName = moduleDetails.getName();
            if (moduleDetails.getModuleType() == ModuleType.CHECK) {
                moduleName += "Check";
            }
            result = Path.of("checkstylemeta-" + moduleName + ".xml");
        }
        return result;
    }
}
