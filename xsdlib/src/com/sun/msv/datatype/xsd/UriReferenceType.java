/*
 * Tranquilo : RELAX Verifier           written by Kohsuke Kawaguchi
 *                                           k-kawa@bigfoot.com
 *
 * Copyright 2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package com.sun.tranquilo.datatype;

import java.io.ByteArrayInputStream;

/**
 * "uriReference" and uriReference-derived types
 * 
 * See http://www.w3.org/TR/xmlschema-2/#uriReference for the spec
 */
public class UriReferenceType extends DataTypeImpl
{
	public static final UriReferenceType theInstance = new UriReferenceType();
	private UriReferenceType() { super("uriReference"); }
	
	protected boolean checkFormat( String content )
	{
		return convertToValue(content)!=null;
	}
	
	public Object convertToValue( String content )
	{
		// we can't use java.net.URL (for example, it cannot handle IPv6.)
		// so use lexical value instead
		
		try
		{// make sure it conforms [RFC2396] (amended by [RFC2732])
			// parser generated by javacc reads a byte sequence, not a character sequence.
			// thus we have to somehow convert a character sequence to a byte sequence.
			
			// currently, URI only accepts US-ASCII characters (#x0-#x7F)
			// (note that this is a priority-feedback item of current draft, so
			// possibility of change is very high)
			
			// By using UTF-8, non ascii characters will have bit image of 1XXXXXXX.
			// thus these characters will be rejected by the parser as an error.
			final UriReferenceParser parser =
				new UriReferenceParser( new ByteArrayInputStream( content.getBytes("UTF-8") ) );
			parser.start();
		}
		catch( Exception e )
		{
			return null;
		}
		
		return content;
	}
	
	public final int isFacetApplicable( String facetName )
	{
		// TODO : should we allow scale facet, or not?
		if( facetName.equals(FACET_LENGTH)
		||	facetName.equals(FACET_MINLENGTH)
		||	facetName.equals(FACET_MAXLENGTH)
		||	facetName.equals(FACET_PATTERN)
		||	facetName.equals(FACET_ENUMERATION) )
			return APPLICABLE;
		else
			return NOT_ALLOWED;
	}
}
