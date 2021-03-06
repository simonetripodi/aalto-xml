package com.fasterxml.aalto.dom;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Context object that holds information about an open element
 * (one for which START_ELEMENT has been sent, but no END_ELEMENT)
 *
 * @author Tatu Saloranta
 */
public class DOMOutputElement
    extends OutputElementBase
{
    /**
     * Reference to the parent element, element enclosing this element.
     * Null for root element.
     * Non-final to allow temporary pooling
     * (on per-writer basis, to keep these short-lived).
     */
    private DOMOutputElement _parent;
    
    /**
     * Actual DOM element for which this element object acts as a proxy.
     */
    private Element _element;

    private boolean _defaultNsSet;

    /**
     * Constructor for the virtual root element
     */
    private DOMOutputElement()
    {
        super();
        _parent = null;
        _element = null;
        _nsMapping = null;
        _nsMapShared = false;
        _defaultNsURI = "";
        _rootNsContext = null;
        _defaultNsSet = false;
    }

    private DOMOutputElement(DOMOutputElement parent,
                             Element element,
                             BijectiveNsMap ns)
    {
        super(parent, ns);
        _parent = parent;
        _element = element;
        _nsMapping = ns;
        _nsMapShared = (ns != null);
        _defaultNsURI = parent._defaultNsURI;
        _rootNsContext = parent._rootNsContext;
        _defaultNsSet = false;
    }

    /**
     * Method called to reuse a pooled instance.
     *
     * @return Chained pooled instance that should now be head of the
     *   reuse chain
     */
    private void relink(DOMOutputElement parent, Element element)
    {
        super.relink(parent);
        _parent = parent;
        _element = element;
        parent.appendNode(element);
        _defaultNsSet = false;
    }

    public static DOMOutputElement createRoot()
    {
        return new DOMOutputElement();
    }
    
    /**
     * Simplest factory method, which gets called when a 1-argument
     * element output method is called. Element is assumed to
     * use the current default namespace.
     * Will both create the child element and attach it to parent element,
     * or lacking own owner document.
     */
    protected DOMOutputElement createAndAttachChild(Element element)
    {
        if(isRoot()) {
            element.getOwnerDocument().appendChild(element);
        } else {
            _element.appendChild(element);
        }
        return createChild(element);
    }

    protected DOMOutputElement createChild(Element element)
    {
        return new DOMOutputElement(this, element, _nsMapping);
    }

    /**
     * @return New head of the recycle pool
     */
    protected DOMOutputElement reuseAsChild(DOMOutputElement parent,
                                               Element element)
    {
        DOMOutputElement poolHead = _parent;
        relink(parent, element);
        return poolHead;
    }

    /**
     * Method called to temporarily link this instance to a pool, to
     * allow reusing of instances with the same reader.
     */
    protected void addToPool(DOMOutputElement poolHead)
    {
        _parent = poolHead;
    }
    
    /*
    ////////////////////////////////////////////
    // Public API, accessors
    ////////////////////////////////////////////
     */

    public DOMOutputElement getParent() {
        return _parent;
    }

    public boolean isRoot() {
        // (Virtual) Root element has no parent...
        return (_parent == null);
    }
    
    /**
     * @return String presentation of the fully-qualified name, in
     *   "prefix:localName" format (no URI). Useful for error and
     *   debugging messages.
     */
    public String getNameDesc() {
        if(_element != null) {
            return _element.getLocalName();
        }
        return "#error"; // unexpected case
    }
    
    /*
    ////////////////////////////////////////////
    // Public API, mutators
    ////////////////////////////////////////////
     */

    public void setDefaultNsUri(String uri) {
        _defaultNsURI = uri;
        _defaultNsSet = true;
    }

    protected void setRootNsContext(NamespaceContext ctxt)
    {
        _rootNsContext = ctxt;
        /* Let's also see if we have an active default ns mapping:
         * (provided it hasn't yet explicitly been set for this element)
         */
        if (!_defaultNsSet) {
            String defURI = ctxt.getNamespaceURI("");
            if (defURI != null && defURI.length() > 0) {
                _defaultNsURI = defURI;
            }
        }
    }

    /*
    ////////////////////////////////////////////
    // Public API, DOM manipulation
    ////////////////////////////////////////////
     */
    
    protected void appendNode(Node n)
    {
        if (isRoot()) {
            _element.getOwnerDocument().appendChild(n);
        } else {
            _element.appendChild(n);
        }
    }

    protected void addAttribute(String pname, String value)
    {
        _element.setAttribute(pname, value);
    }

    protected void addAttribute(String uri, String qname, String value)
    {
        _element.setAttributeNS(uri, qname, value);
    }

    public void appendChild(Node n) {
        _element.appendChild(n);
    }
}
