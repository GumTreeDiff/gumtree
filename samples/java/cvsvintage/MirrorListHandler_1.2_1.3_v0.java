/*
 * MirrorListHandler.java - XML handler for the mirrors list
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki (parts copied from Slava Pestov :) )
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.pluginmgr;

import java.io.*;
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.Log;

class MirrorListHandler extends DefaultHandler
{
	//{{{ Constructor
	MirrorListHandler(MirrorList mirrors, String path)
	{
		this.mirrors = mirrors;
		this.path = path;
		stateStack = new Stack();

		description = new StringBuffer();
		location = new StringBuffer();
		country = new StringBuffer();
		continent = new StringBuffer();
	} //}}}

	//{{{ resolveEntity() method
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return MiscUtilities.findEntity(systemId, "mirrors.dtd",
					org.gjt.sp.jedit.options.PluginOptions.class);
	} //}}}

	//{{{ characters() method
	public void characters(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if(tag == "DESCRIPTION")
			description.append(c, off, len);
		else if(tag == "LOCATION")
			location.append(c, off, len);
		else if(tag == "COUNTRY")
			country.append(c, off, len);
		else if(tag == "CONTINENT")
			continent.append(c, off, len);
	} //}}}

	//{{{ startElement() method
	public void startElement(String uri, String localName,
				 String tag, Attributes attrs)
	{
		tag = pushElement(tag);

		if(tag.equals("MIRROR"))
			mirror = new MirrorList.Mirror();
		id = attrs.getValue("ID");
	} //}}}

	//{{{ endElement() method
	public void endElement(String uri, String localName, String tag)
	{
		popElement();

		if(tag.equals("MIRROR"))
		{
			mirror.id = id;
			mirror.description = description.toString();
			mirror.location = location.toString();
			mirror.country = country.toString();
			mirror.continent = continent.toString();
			mirrors.add(mirror);
			description.setLength(0);
			location.setLength(0);
			country.setLength(0);
			continent.setLength(0);
		}
	} //}}}

	//{{{ startDocument() method
	public void startDocument()
	{
		try
		{
			pushElement(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	} //}}}

	//{{{ endDocument() method
	public void endDocument()
	{
		mirrors.finished();
	} //}}}

	//{{{ Private members

	//{{{ Variables
	private String id;
	private StringBuffer description;
	private StringBuffer location;
	private StringBuffer country;
	private StringBuffer continent;

	private MirrorList mirrors;
	private MirrorList.Mirror mirror;

	private Stack stateStack;
	private String path;
	//}}}

	private String pushElement(String name)
	{
		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	}

	private String peekElement()
	{
		return (String) stateStack.peek();
	}

	private String popElement()
	{
		return (String) stateStack.pop();
	}

	//}}}
}
