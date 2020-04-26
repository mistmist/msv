/*
 * @(#)$Id$
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.tahiti.grammar;

import com.sun.msv.grammar.OtherExp;

/**
 * the base class of all special OtherExps
 * that are used to annotate tahiti data-binding information
 * to AGM.
 * 
 * @author
 *	<a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public abstract class JavaItem extends OtherExp {
	public JavaItem( String name ) {
		this.name = name;
	}
	
	public String name;
	
	public abstract Object visitJI( JavaItemVisitor visitor );
}
