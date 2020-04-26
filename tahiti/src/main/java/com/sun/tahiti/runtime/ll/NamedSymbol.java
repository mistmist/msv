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

/**
 * Non-terminal name symbol for LL grammar.
 * immutable.
 * 
 * @author
 *	<a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class NamedSymbol extends NonTerminalSymbol
{
	public final String name;
	public NamedSymbol( String name ) {
		this.name = name;
	}
	public String toString() {
		return "N<"+name+">";
	}
	
	public LLParser.Receiver createReceiver( final LLParser.Receiver parent ) {
		return new LLParser.ObjectReceiver() {
			public void start() throws Exception {}
			public void end() throws Exception {}
			public void action( Object item ) throws Exception {
				((LLParser.FieldReceiver)parent).action( item, NamedSymbol.this );
			}
		};
	}
}
