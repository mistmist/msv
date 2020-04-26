/*
 * @(#)$Id$
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.tahiti.runtime.ll;

import com.sun.msv.datatype.DatabindableDatatype;
import org.relaxng.datatype.ValidationContext;

/**
 * Intermediate non-terminal symbols for LL grammar.
 * 
 * Intermediate non-terminals are non-terminals with no associated action.
 * 
 * @author
 *	<a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public final class IntermediateSymbol extends NonTerminalSymbol
{
	/** purely for the debug purpose. */
	public final String identifier;
	public IntermediateSymbol( String identifier ) {
		this.identifier = identifier;
	}
	public String toString() {
		return identifier;
	}
	
	/**
	 * Intermediate non-terminal does not have its own action.
	 */
	public LLParser.Receiver createReceiver( final LLParser.Receiver parent ) {
		// so it can simply return the parent receiver.
		return parent;
	}
}
