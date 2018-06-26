package solution;


import org.apache.log4j.BasicConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class XmlAnalyzer {

    private static final String CHARSET_NAME = "utf8";
    private static final String DEFAULT_BUTTON_ID = "make-everything-ok-button";
    private static final String ALL_BUTTONS_QUERY = ".btn";
    private static Logger LOGGER = LoggerFactory.getLogger(XmlAnalyzer.class);

    public static void main(String[] args) {

        BasicConfigurator.configure();
        String originalHtmlPath = args[0];
        String diffCaseHtmlPath = args[1];
        String originalElementId;

        if (args.length == 2) {
            originalElementId = DEFAULT_BUTTON_ID;
        } else {
            originalElementId = args[2];
        }

        // Getting original element from the html file
        Optional<Element> buttonOpt = findElementById(new File(originalHtmlPath), originalElementId);

        Optional<String> stringifiedAttributesOpt = buttonOpt.map(button ->
                button.attributes().asList().stream()
                        .map(attr -> attr.getKey() + " = " + attr.getValue())
                        .collect(Collectors.joining(", "))
        );

        // Logging all element attributes
        stringifiedAttributesOpt.ifPresent(attrs -> LOGGER.info("Original element attributes: [{}]", attrs));

        // Creating set of strings with values of attributes
        Set<String> originalAttributes = new HashSet<>();
        buttonOpt.ifPresent(button -> originalAttributes
                .addAll(button.attributes().asList().stream()
                        .map(Attribute::getValue)
                        .collect(Collectors.toSet())));

        Optional<Elements> elementsOpt = findElementsByQuery(new File(diffCaseHtmlPath), ALL_BUTTONS_QUERY);

        if (!elementsOpt.isPresent()) {
            throw new RuntimeException("No such elements");
        }
        Elements targetElements = elementsOpt.get();

        ElementWrapper targetElement = getTargetElement(targetElements, originalAttributes);

        pathBuilder(targetElement);

    }

    private static Optional<Element> findElementById(File originalHtml, String originalElementId) {
        try {
            Document doc = Jsoup.parse(
                    originalHtml,
                    CHARSET_NAME,
                    originalHtml.getAbsolutePath());

            return Optional.of(doc.getElementById(originalElementId));

        } catch (IOException e) {
            LOGGER.error("Error reading [{}] file", originalHtml.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    private static Optional<Elements> findElementsByQuery(File diffCaseHtml, String cssQuery) {
        try {
            Document doc = Jsoup.parse(
                    diffCaseHtml,
                    CHARSET_NAME,
                    diffCaseHtml.getAbsolutePath());

            return Optional.of(doc.select(cssQuery));

        } catch (IOException e) {
            LOGGER.error("Error reading [{}] file", diffCaseHtml.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    private static ElementWrapper getTargetElement(Elements elements, Set<String> originalAttributes) {
        List<ElementWrapper> elementWrappers = new ArrayList<>();
        elements.forEach(element -> elementWrappers
                .add(new ElementWrapper(element, element.attributes()
                        .asList()
                        .stream()
                        .map(Attribute::getValue)
                        .collect(Collectors.toSet()))));
        int max = 0;
        ElementWrapper targetElement = null;
        for (ElementWrapper elementWrapper : elementWrappers) {
            Set<String> intersection = new HashSet<>(elementWrapper.getAttributeSet());
            intersection.retainAll(originalAttributes);
            if (max < intersection.size()) {
                max = intersection.size();
                targetElement = elementWrapper;
            }
        }
        return targetElement;
    }

    private static void pathBuilder(ElementWrapper elementWrapper) {
        System.out.println("----------------------------------------");

        if (elementWrapper != null) {
            System.out.println("Target element attributes: " + elementWrapper.getAttributeSet());
            System.out.println("\nPath to target element: " + "html > body > div.wrapper > div."
                    + elementWrapper.getElement().cssSelector());
        }
    }

    static class ElementWrapper {

        private Element element;
        private Set<String> attributeSet;

        public ElementWrapper(Element element, Set<String> attributeSet) {
            this.element = element;
            this.attributeSet = attributeSet;
        }

        public Element getElement() {
            return element;
        }

        public Set<String> getAttributeSet() {
            return attributeSet;
        }

        public void setElement(Element element) {
            this.element = element;
        }

        public void setAttributeSet(Set<String> attributeSet) {
            this.attributeSet = attributeSet;
        }
    }
}