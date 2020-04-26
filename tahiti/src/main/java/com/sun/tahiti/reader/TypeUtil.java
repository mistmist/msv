/*
 * @(#)$Id$
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.tahiti.reader;

import com.sun.tahiti.grammar.*;
import java.util.Set;

/**
 * Type-related utility methods.
 * 
 * @author
 *	<a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class TypeUtil {
	
	/**
	 * compute the common base type of types.
	 * 
	 * TODO: this is a very interesting problem. Since one type has possibly
	 * multiple base types, it's not an easy problem.
	 * The current implementation is very naive.
	 */
	public static Type getCommonBaseType( Type[] t ) {
		// TODO:
		
		for( int i=1; i<t.length; i++ )
			if(!t[0].getTypeName().equals(t[i].getTypeName()))
				return SystemType.get(Object.class);
		
		return t[0];
	}
	
	/**
	 * compute the common base type of two types.
	 * 
	 * @param types
	 *		set of {@link Type} objects.
	 */
	public static Type getCommonBaseType( Set types ) {
		return getCommonBaseType( (Type[])types.toArray(new Type[types.size()]) );
	}
}
