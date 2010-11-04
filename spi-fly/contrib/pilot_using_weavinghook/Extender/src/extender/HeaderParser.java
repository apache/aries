package extender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to parse a standard OSGi header with paths.
 *
 * Stolen from Aries Blueprint Core (blueprint.utils). Need to give it back! 
 */
public class HeaderParser  {

    /**
     * Parse a given OSGi header into a list of paths
     *
     * @param header the OSGi header to parse
     * @return the list of paths extracted from this header
     */
    public static List<PathElement> parseHeader(String header) {
        List<PathElement> elements = new ArrayList<PathElement>();
        if (header == null || header.trim().length() == 0) {
            return elements;
        }
        String[] clauses = header.split(",");
        for (String clause : clauses) {
            String[] tokens = clause.split(";");
            if (tokens.length < 1) {
                throw new IllegalArgumentException("Invalid header clause: " + clause);
            }
            PathElement elem = new PathElement(tokens[0].trim());
            elements.add(elem);
            for (int i = 1; i < tokens.length; i++) {
                int pos = tokens[i].indexOf('=');
                if (pos != -1) {
                    if (pos > 0 && tokens[i].charAt(pos - 1) == ':') {
                        String name = tokens[i].substring(0, pos - 1).trim();
                        String value = tokens[i].substring(pos + 1).trim();
                        elem.addDirective(name, value);
                    } else {
                        String name = tokens[i].substring(0, pos).trim();
                        String value = tokens[i].substring(pos + 1).trim();
                        elem.addAttribute(name, value);
                    }
                } else {
                    elem = new PathElement(tokens[i].trim());
                    elements.add(elem);
                }
            }
        }
        return elements;
    }

    public static class PathElement {
        
        private String path;
        private Map<String, String> attributes;
        private Map<String, String> directives;
        
        public PathElement(String path) {
            this.path = path;
            this.attributes = new HashMap<String, String>();
            this.directives = new HashMap<String, String>();
        }
        
        public String getName() {
            return this.path;
        }
        
        public Map<String, String> getAttributes() {
            return attributes;
        }
        
        public String getAttribute(String name) {
            return attributes.get(name);
        }
        
        public void addAttribute(String name, String value) {
            attributes.put(name, value);
        }
        
        public Map<String, String> getDirectives() {
            return directives;
        }
        
        public String getDirective(String name) {
            return directives.get(name);
        }
        
        public void addDirective(String name, String value) {
            directives.put(name, value);
        }        
        
    }
}
