/*
 * Copyright (c) 2006, John Kristian
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *      *   Redistributions of source code must retain the above copyright
 *          notice, this list of conditions and the following disclaimer.
 *
 *      *   Redistributions in binary form must reproduce the above copyright
 *          notice, this list of conditions and the following disclaimer in the
 *          documentation and/or other materials provided with the distribution.
 *
 *      *   Neither the name of StAX-Utils nor the names of its contributors
 *          may be used to endorse or promote products derived from this
 *          software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.github.gumtreediff.io;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Abstract class for writing filtered XML streams. This class provides methods
 * that merely delegate to the contained stream. Subclasses should override some
 * of these methods, and may also provide additional methods and fields.
 *
 * @author <a href="mailto:jk2006@engineer.com">John Kristian</a>
 */
public abstract class StreamWriterDelegate implements XMLStreamWriter {

    protected StreamWriterDelegate(XMLStreamWriter out) {
        this.out = out;
    }

    protected XMLStreamWriter out;

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return out.getProperty(name);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return out.getNamespaceContext();
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        out.setNamespaceContext(context);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        out.setDefaultNamespace(uri);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        out.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        out.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        out.writeStartDocument(encoding, version);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        out.writeDTD(dtd);
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        out.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        out.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        out.writeComment(data);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        out.writeEmptyElement(localName);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        out.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException {
        out.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        out.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        out.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException {
        out.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        out.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        out.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return out.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        out.setPrefix(prefix, uri);
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        out.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value)
            throws XMLStreamException {
        out.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException {
        out.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        out.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        out.writeCharacters(text, start, len);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        out.writeCData(data);
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        out.writeEntityRef(name);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        out.writeEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        out.writeEndDocument();
    }

    @Override
    public void flush() throws XMLStreamException {
        out.flush();
    }

    @Override
    public void close() throws XMLStreamException {
        out.close();
    }

}
