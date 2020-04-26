/*
 * @(#)$Id$
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.tahiti.compiler.java;

import com.sun.msv.grammar.*;
import com.sun.tahiti.compiler.Symbolizer;
import com.sun.tahiti.compiler.Controller;
import com.sun.tahiti.compiler.XMLWriter;
import com.sun.tahiti.compiler.sm.MarshallerGenerator;
import com.sun.tahiti.grammar.*;
import com.sun.tahiti.grammar.util.Multiplicity;
import com.sun.tahiti.reader.NameUtil;
import com.sun.tahiti.util.text.Formatter;
import com.sun.tahiti.util.xml.XSLTUtil;
import com.sun.tahiti.util.xml.DOMVisitor;
import com.sun.tahiti.util.xml.DOMBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.text.MessageFormat;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * produces Java source codes of the object model.
 * 
 * @author
 *	<a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ClassSerializer
{
	public ClassSerializer( AnnotatedGrammar grammar, Symbolizer symbolizer, Controller controller ) {
		this.grammar = grammar;
		this.symbolizer = symbolizer;
		this.controller = controller;
		this.grammarClassName = grammar.grammarName;
		
		int idx = grammarClassName.lastIndexOf('.');
		if(idx<0)	grammarShortClassName = grammarClassName;
		else		grammarShortClassName = grammarClassName.substring(idx+1);
	}
	
	private final AnnotatedGrammar grammar;
	private final Symbolizer symbolizer;
	private final Controller controller;
	
	/** fully-qualified name of the Gramamr class. */
	protected final String grammarClassName;
	/**
	 * bare class name of the Grammar class.
	 * Since the Grammar class is imported in generated classes,
	 * this bare name can be used to refer to the Grammar class.
	 */
	protected final String grammarShortClassName;
		
	public void generate() throws IOException {
		// collect all ClassItems.
		
		ClassItem[] types = grammar.getClasses();
		for( int i=0; i<types.length; i++ ) {
			out = new PrintWriter(controller.getOutput(types[i]));
			writeClass( types[i] );
		}
		
		InterfaceItem[] itfs = grammar.getInterfaces();
		for( int i=0; i<itfs.length; i++ ) {
			out = new PrintWriter(controller.getOutput(itfs[i]));
			writeClass( itfs[i] );
		}
	}
	
	
	private static String format( String fmt, Object[] args ) {
		return MessageFormat.format(fmt,args);
	}
	private static String format( String fmt, Object arg1 ) {
		return MessageFormat.format(fmt,new Object[]{arg1});
	}
	private static String format( String fmt, Object arg1, Object arg2 ) {
		return MessageFormat.format(fmt,new Object[]{arg1,arg2});
	}
	private static String format( String fmt, Object arg1, Object arg2, Object arg3 ) {
		return MessageFormat.format(fmt,new Object[]{arg1,arg2,arg3});
	}
	private static String format( String fmt, Object arg1, Object arg2, Object arg3, Object arg4 ) {
		return MessageFormat.format(fmt,new Object[]{arg1,arg2,arg3,arg4});
	}
	
	/**
	 * computes the type name that will can be placed into the source code.
	 * 
	 * If the given type obejct is in the current package, then only the local type
	 * name is returned. Otherwise fully qualified name is returned.
	 */
	protected String toPrintName( Type type ) {
		if( packageName==type.getPackageName()
		|| 	(packageName!=null && packageName.equals( type.getPackageName() ))
		||  "java.lang".equals( type.getPackageName() ) )
			return type.getBareName();
		else
			return type.getTypeName();
	}
	
	/**
	 * the name of the package into which this class is placed.
	 * This field is set to null to indicate the root package.
	 */
	private String packageName;
	
	/**
	 * output object which receives the produced Java source code.
	 * Set in the generate method.
	 */
	private PrintWriter out;
	
	/**
	 * writes the body of a ClassItem.
	 */
	private void writeClass( TypeItem type ) {

		// one 
		ClassItem citm = null;
		InterfaceItem iitm = null;
		
		if( type instanceof ClassItem )	citm = (ClassItem)type;
		else							iitm = (InterfaceItem)type;
	
		
		packageName = type.getPackageName();
		if( packageName!=null )
			out.println(format("package {0};\n",packageName));
		
		out.println("import com.sun.tahiti.runtime.ll.NamedSymbol;");
		out.println("import com.sun.tahiti.runtime.sm.Marshaller;");
		// if the grammar class is placed under some package
		if(!grammarClassName.equals(grammarShortClassName))
			out.println(format("import {0};",grammarClassName));
		out.println();
		
		
		produceHeaderComment(type);

		
	// class name and the base class definitions
	//-------------------------------------------
		
		out.print(format("public {0} {1}",
			citm!=null?"class":"interface",	type.getBareName() ));
		
		if( citm!=null && citm.getSuperType()!=null )
			out.print(format(" extends {0}",
				toPrintName( type.getSuperType() ) ));
		
		Type[] itfs = type.getInterfaces();
		if(itfs.length!=0) {
			out.print(format(" {0} {1}",
				citm!=null?"implements":"extends",
				toPrintName(itfs[0])));
			
			for( int i=1;i<itfs.length;i++ )
				out.print(format(", {0}",toPrintName(itfs[i])));
		}
		
		out.println(" {");
		out.println();

		
		
	// prepare field serializers
	//----------------------------------------
		FieldUse[] fields;
		{
			Vector f = new Vector();
			TypeItem t = type;
			while(true) {
				f.addAll( t.fields.values() );
				if( t instanceof ClassItem ) {
					ClassItem c = (ClassItem)t;
					if(c.superClass!=null) {
						t = c.superClass.definition;
						continue;
					}
				}
				break;
			}
			fields = (FieldUse[])f.toArray(new FieldUse[0]);
		}
		FieldSerializer[] fieldSerializers = new FieldSerializer[fields.length];

		// a map from field name to FieldSerializer.
		final Map fsMap = new java.util.HashMap();
		
		for( int i=0; i<fields.length; i++ ) {
			fieldSerializers[i] = FieldSerializer.get(this,fields[i]);
			fsMap.put( fields[i].name, fieldSerializers[i] );
		}
			
		
	// generate fields
	//----------------------------------------
		for( int i=0; i<fields.length; i++ ) {
			out.println(
				"\n"+
				"//\n"+
				"// "+fields[i].name+"\n"+
				"//\n");
			fieldSerializers[i].writeFieldDef(out);
		}
		
	// generate the setField method
	//------------------------------------------
		if( citm!=null ) {
			out.println("\n\n");
			out.println(
				"\t/**\n"+
				"\t * unmarshalling handler.\n"+
				"\t * This method is called to unmarshall objects from XML.\n"+
				"\t */");
			out.println("\tpublic void setField( NamedSymbol name, Object item ) throws Exception {");
			for( int i=0; i<fields.length; i++ ) {
				
				FieldUse fu = fields[i];
				FieldSerializer fs = fieldSerializers[i];
				
				out.print("\t\tif( ");
				FieldItem[] fi = fu.getItems();
				for( int j=0; j<fi.length; j++ ) {
					if(j!=0)
						out.print(" || ");
					out.print(format("name=={0}.{1}",
						grammarShortClassName,
						symbolizer.getId(fi[j])));
				}
				out.println(" ) {");
				out.println("\t\t\t"+fs.setField("item"));
				out.println("\t\t\treturn;");
				out.println("\t\t}");
			}
			if( type.getSuperType()!=null )
				out.println("\t\tsuper.setField(name,item);");
			else
				out.println("\t\tthrow new Error();//assertion failed.this is not possible");
			out.println("\t}");
		}

	// generate the marshaller
	//------------------------------------------
		if( citm!=null ) {
			try {
				// TODO: ideally, a marshaller should be produced as a separate class,
				// to minimize dependency between the marshaller and the unmarshaller.
				// however, since we have only one marshaller at this moment,
				// we will create it here.
				
/*				TransformerHandler xsltEngine = XSLTUtil.getTransformer(
					JavaGenerator.class.getResourceAsStream("marshaller2java.xsl"));
				xsltEngine.setResult( new StreamResult(out) );
				XMLWriter writer = XMLWriter.fromContentHandler(xsltEngine);
		
				// call MarshallerGenerator to have it produce marshaller.
				// generated marshaller will be then transformed into Java source code
				// by using XSLT.
				xsltEngine.startDocument();
				MarshallerGenerator.write( citm, writer, controller );
				xsltEngine.endDocument();
*/
				// get DOM representation of the marshaller
				DOMBuilder builder = new DOMBuilder();
				XMLWriter writer = XMLWriter.fromContentHandler(builder);
				writer.handler.startDocument();
				MarshallerGenerator.write( symbolizer, citm, writer, controller );
				writer.handler.endDocument();
				
				// produce a source code fragment from DOM.
				MarshallerSerializer.write( fsMap, out, builder.getDocument() );
				
			} catch( MarshallerGenerator.Abort a ) {
				// generation of the marshaller is aborted.
				out.println(
					"\t// Tahiti fails to produce a marshaller for this class.\n"+
					"\t// You have to implement one by yourself if you need it.\n"+
					"\tpublic void marshall( Marshaller out ) {\n"+
					"\t\tthrow new UnsupportedOperationException();\n"+
					"\t}\n");
			} catch( javax.xml.parsers.ParserConfigurationException e ) {
				controller.error( null, e.getMessage(), e );
				// TODO: what shall I do if an error happens.
			} catch( SAXException e ) {
				controller.error( null, e.getMessage(), e );
				// TODO: what shall I do if an error happens.
			}
		} else {
			// in case of an interface, produce a signature.
			out.println("\tvoid marshall( Marshaller out );\n");
		}
		
		
		
		out.println("}");
		out.flush();
		out.close();
	}
	
	/**
	 * produces a javadoc comment for this type.
	 */
	protected void produceHeaderComment( TypeItem type ) {
		out.println("/**");
		out.println(" * "+type.getBareName()+".");
		
		// lists derived classes.
		boolean hasDerivedType = false;
		out.println(" * <h2>list of derived types</h2>");
		hasDerivedType |= listDerivedTypes(type,grammar.iterateClasses());
		hasDerivedType |= listDerivedTypes(type,grammar.iterateInterfaces());
		if(!hasDerivedType)
			out.println(" *  no derived type");
		out.println(" */");
	}
	
	protected boolean listDerivedTypes( TypeItem current, Iterator itr ) {
		boolean hasDerivedType = false;
		while( itr.hasNext() ) {
			TypeItem type = (TypeItem)itr.next();
			
			boolean isDerived = false;
			
			if(type.getSuperType()==current)
				isDerived = true;
			
			Type[] itfs = type.getInterfaces();
			for( int i=0; i<itfs.length; i++ )
				if( itfs[i]==current )
					isDerived = true;
			
			if(isDerived)
				out.println(" *   {@link "+type.getTypeName()+"}");
			hasDerivedType |= isDerived;
		}
		return hasDerivedType;
	}
}
