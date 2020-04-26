/*
 * @(#)$Id$
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.schmit.reader.relaxng;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

import com.sun.msv.grammar.Expression;
import com.sun.msv.reader.State;
import com.sun.msv.reader.trex.ng.AttributeState;
import com.sun.msv.reader.trex.ng.RELAXNGReader;
import com.sun.msv.schmit.grammar.relaxng.AnnotatedAttributePattern;
import com.sun.msv.schmit.reader.*;
import com.sun.msv.util.StartTagInfo;

/**
 * 
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class SchmitAttributeState extends AttributeState implements AnnotationParent {
    /** Parsed annotations. */
    private final List annotations = new ArrayList();

    protected State createChildState(StartTagInfo tag) {
        if( !RELAXNGReader.RELAXNGNamespace.equals(tag.namespaceURI) )
            // parse it as an annotation
            return new AnnotationState( ((SchmitRELAXNGReader)reader).dom );
        else
            return super.createChildState(tag);
    }

    public void onEndAnnotation(Node annotation) {
        annotations.add(annotation);
    }
    
    protected void startSelf() {
        super.startSelf();
        ((SchmitRELAXNGReader)reader).parseAttributeAnnotation( startTag, this );
    }


    protected Expression annealExpression( Expression contentModel ) {
        AnnotatedAttributePattern e =
            new AnnotatedAttributePattern( nameClass, contentModel, annotations );
        reader.setDeclaredLocationOf(e);
        return e;
    }

    protected boolean isGrammarElement(StartTagInfo tag) {
        // we will need to read everything
        return true;
    }
}
