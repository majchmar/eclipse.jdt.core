/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ICompletionRequestor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @see IClassFile
 */

public class ClassFile extends Openable implements IClassFile, SuffixConstants {
	protected BinaryType binaryType = null;
	private boolean checkAutomaticSourceMapping;
/*
 * Creates a handle to a class file.
 */
protected ClassFile(PackageFragment parent, String name) {
	super(parent, name);
	this.checkAutomaticSourceMapping = false;
}

/**
 * Creates the children elements for this class file adding the resulting
 * new handles and info objects to the newElements table. Returns true
 * if successful, or false if an error is encountered parsing the class file.
 * 
 * @see Openable
 * @see Signature
 */
protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaModelException {
	// check whether the class file can be opened
	if (!isValidClassFile()) throw newNotPresentException();
	if (underlyingResource != null && !underlyingResource.isAccessible()) throw newNotPresentException();

	IBinaryType typeInfo = getBinaryTypeInfo((IFile) underlyingResource);
	if (typeInfo == null) {
		// The structure of a class file is unknown if a class file format errors occurred
		//during the creation of the diet class file representative of this ClassFile.
		info.setChildren(new IJavaElement[] {});
		return false;
	}

	// Make the type
	IType type = new BinaryType(this, new String(simpleName(typeInfo.getName())));
	info.addChild(type);
	newElements.put(type, typeInfo);
	return true;
}
/**
 * @see ICodeAssist#codeComplete(int, ICompletionRequestor)
 */
public void codeComplete(int offset, ICompletionRequestor requestor) throws JavaModelException {
	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see ICodeAssist#codeComplete(int, ICompletionRequestor, WorkingCopyOwner)
 */
public void codeComplete(int offset, ICompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	String source = getSource();
	if (source != null) {
		String encoding = this.getJavaProject().getOption(JavaCore.CORE_ENCODING, true);
		String elementName = getElementName();
		BasicCompilationUnit cu = 
			new BasicCompilationUnit(
				getSource().toCharArray(), 
				null,
				elementName.substring(0, elementName.length()-SUFFIX_STRING_class.length()) + SUFFIX_STRING_java,
				encoding); 
		codeComplete(cu, cu, offset, requestor, owner);
	}
}
/**
 * @see ICodeAssist#codeSelect(int, int)
 */
public IJavaElement[] codeSelect(int offset, int length) throws JavaModelException {
	return codeSelect(offset, length, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see ICodeAssist#codeSelect(int, int, WorkingCopyOwner)
 */
public IJavaElement[] codeSelect(int offset, int length, WorkingCopyOwner owner) throws JavaModelException {
	IBuffer buffer = getBuffer();
	char[] contents;
	if (buffer != null && (contents = buffer.getCharacters()) != null) {
	    String topLevelTypeName = getTopLevelTypeName();
		BasicCompilationUnit cu = new BasicCompilationUnit(contents, null, topLevelTypeName + SUFFIX_STRING_java, null);
		return super.codeSelect(cu, offset, length, owner);
	} else {
		//has no associated souce
		return new IJavaElement[] {};
	}
}
/**
 * Returns a new element info for this element.
 */
protected Object createElementInfo() {
	return new ClassFileInfo(this);
}
public boolean equals(Object o) {
	if (!(o instanceof ClassFile)) return false;
	return super.equals(o);
}
public boolean exists() {
	if (!isValidClassFile()) return false;
	return super.exists();
}

/**
 * Finds the deepest <code>IJavaElement</code> in the hierarchy of
 * <code>elt</elt>'s children (including <code>elt</code> itself)
 * which has a source range that encloses <code>position</code>
 * according to <code>mapper</code>.
 */
protected IJavaElement findElement(IJavaElement elt, int position, SourceMapper mapper) {
	SourceRange range = mapper.getSourceRange(elt);
	if (range == null || position < range.getOffset() || range.getOffset() + range.getLength() - 1 < position) {
		return null;
	}
	if (elt instanceof IParent) {
		try {
			IJavaElement[] children = ((IParent) elt).getChildren();
			for (int i = 0; i < children.length; i++) {
				IJavaElement match = findElement(children[i], position, mapper);
				if (match != null) {
					return match;
				}
			}
		} catch (JavaModelException npe) {
			// elt doesn't exist: return the element
		}
	}
	return elt;
}
/**
 * Returns the <code>ClassFileReader</code>specific for this IClassFile, based
 * on its underlying resource, or <code>null</code> if unable to create
 * the diet class file.
 * There are two cases to consider:<ul>
 * <li>a class file corresponding to an IFile resource</li>
 * <li>a class file corresponding to a zip entry in a JAR</li>
 * </ul>
 *
 * @exception JavaModelException when the IFile resource or JAR is not available
 * or when this class file is not present in the JAR
 */
public IBinaryType getBinaryTypeInfo(IFile file) throws JavaModelException {
	JavaElement le = (JavaElement) getParent();
	if (le instanceof JarPackageFragment) {
		try {
			JarPackageFragmentRoot root = (JarPackageFragmentRoot) le.getParent();
			IBinaryType info = null;
			ZipFile zip = null;
			try {
				zip = root.getJar();
				String entryName = getParent().getElementName();
				entryName = entryName.replace('.', '/');
				if (entryName.equals("")) { //$NON-NLS-1$
					entryName += getElementName();
				} else {
					entryName += '/' + getElementName();
				}
				info = ClassFileReader.read(zip, entryName, true);
			} finally {
				JavaModelManager.getJavaModelManager().closeZipFile(zip);
			}
			if (info == null) {
				throw newNotPresentException();
			}
			return info;
		} catch (ClassFormatException cfe) {
			//the structure remains unknown
			return null;
		} catch (IOException ioe) {
			throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
		} catch (CoreException e) {
			if (e instanceof JavaModelException) {
				throw (JavaModelException)e;
			} else {
				throw new JavaModelException(e);
			}
		}
	} else {
		byte[] contents = Util.getResourceContentsAsByteArray(file);
		try {
			return new ClassFileReader(contents, getElementName().toCharArray());
		} catch (ClassFormatException cfe) {
			//the structure remains unknown
			return null;
		}
	}
}
public IBuffer getBuffer() throws JavaModelException {
	if (isValidClassFile()) {
		return super.getBuffer();
	} else {
		// .class file not on classpath, create a new buffer to be nice (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=41444)
		return openBuffer(null, null);
	}
}
/**
 * @see IMember
 */
public IClassFile getClassFile() {
	return this;
}
/**
 * A class file has a corresponding resource unless it is contained
 * in a jar.
 *
 * @see IJavaElement
 */
public IResource getCorrespondingResource() throws JavaModelException {
	IPackageFragmentRoot root= (IPackageFragmentRoot)getParent().getParent();
	if (root.isArchive()) {
		return null;
	} else {
		return getUnderlyingResource();
	}
}
/**
 * @see IClassFile
 */
public IJavaElement getElementAt(int position) throws JavaModelException {
	IJavaElement parentElement = getParent();
	while (parentElement.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
		parentElement = parentElement.getParent();
	}
	PackageFragmentRoot root = (PackageFragmentRoot) parentElement;
	SourceMapper mapper = root.getSourceMapper();
	if (mapper == null) {
		return null;
	} else {
		// ensure this class file's buffer is open so that source ranges are computed
		getBuffer();

		IType type = getType();
		return findElement(type, position, mapper);
	}
}
/**
 * @see IJavaElement
 */
public int getElementType() {
	return CLASS_FILE;
}
/*
 * @see JavaElement
 */
public IJavaElement getHandleFromMemento(String token, StringTokenizer memento, WorkingCopyOwner owner) {
	switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, owner);
		case JEM_TYPE:
			String typeName = memento.nextToken();
			JavaElement type = new BinaryType(this, typeName);
			return type.getHandleFromMemento(memento, owner);
	}
	return null;
}
/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_CLASSFILE;
}
/*
 * @see IJavaElement
 */
public IPath getPath() {
	PackageFragmentRoot root = this.getPackageFragmentRoot();
	if (root.isArchive()) {
		return root.getPath();
	} else {
		return this.getParent().getPath().append(this.getElementName());
	}
}
/*
 * @see IJavaElement
 */
public IResource getResource() {
	PackageFragmentRoot root = this.getPackageFragmentRoot();
	if (root.isArchive()) {
		return root.getResource();
	} else {
		return ((IContainer)this.getParent().getResource()).getFile(new Path(this.getElementName()));
	}
}
/**
 * @see ISourceReference
 */
public String getSource() throws JavaModelException {
	IBuffer buffer = getBuffer();
	if (buffer == null) {
		return null;
	}
	return buffer.getContents();
}
/**
 * @see ISourceReference
 */
public ISourceRange getSourceRange() throws JavaModelException {
	IBuffer buffer = getBuffer();
	if (buffer != null) {
		String contents = buffer.getContents();
		if (contents == null) return null;
		return new SourceRange(0, contents.length());
	} else {
		return null;
	}
}
/*
 * Returns the name of the toplevel type of this class file.
 */
public String getTopLevelTypeName() {
    String topLevelTypeName = getElementName();
    int firstDollar = topLevelTypeName.indexOf('$');
    if (firstDollar != -1) {
        topLevelTypeName = topLevelTypeName.substring(0, firstDollar);
    } else {
        topLevelTypeName = topLevelTypeName.substring(0, topLevelTypeName.length()-SUFFIX_CLASS.length);
    }
    return topLevelTypeName;
}
/**
 * @see IClassFile
 */
public IType getType() {
	if (this.binaryType == null) {
		// Remove the ".class" from the name of the ClassFile - always works
		// since constructor fails if name does not end with ".class"
		String typeName = this.name.substring(0, this.name.lastIndexOf('.'));
		typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
		int index = typeName.lastIndexOf('$');
		if (index > -1) {
			if (typeName.length() > (index + 1) && !Character.isDigit(typeName.charAt(index + 1))) {
				typeName = typeName.substring(index + 1);
			}
		}
		this.binaryType = new BinaryType(this, typeName);
	}
	return this.binaryType;
}
/*
 * @see IClassFile
 */
public ICompilationUnit getWorkingCopy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
	// get the source if possible
	char[] contents = null;
	SourceMapper mapper = this.getSourceMapper();
	if (mapper != null) {
		contents = mapper.findSource(getType());
	}
	if (contents == null) {
		return null;
	}

	ClassFileWorkingCopy workingCopy = new ClassFileWorkingCopy();
	IBuffer buffer = owner == null ? this.getBuffer() : owner.createBuffer(workingCopy);
	workingCopy.buffer = buffer;
	
	// set the buffer source
	if (buffer != null && buffer.getCharacters() == null){
		buffer.setContents(contents);
	}
	return workingCopy;
}
/**
 * @see IClassFile
 * @deprecated
 */
public IJavaElement getWorkingCopy(IProgressMonitor monitor, org.eclipse.jdt.core.IBufferFactory factory) throws JavaModelException {
	return getWorkingCopy(BufferFactoryWrapper.create(factory), monitor);
}
/**
 * @see Openable
 */
protected boolean hasBuffer() {
	return true;
}
/**
 * If I am not open, return true to avoid parsing.
 *
 * @see IParent 
 */
public boolean hasChildren() throws JavaModelException {
	if (isOpen()) {
		return getChildren().length > 0;
	} else {
		return true;
	}
}
/**
 * @see IClassFile
 */
public boolean isClass() throws JavaModelException {
	return getType().isClass();
}
/**
 * @see IClassFile
 */
public boolean isInterface() throws JavaModelException {
	return getType().isInterface();
}
/**
 * Returns true - class files are always read only.
 */
public boolean isReadOnly() {
	return true;
}
private boolean isValidClassFile() {
	IPackageFragmentRoot root = getPackageFragmentRoot();
	try {
		if (root.getKind() != IPackageFragmentRoot.K_BINARY) return false;
	} catch (JavaModelException e) {
		return false;
	}
	if (!Util.isValidClassFileName(getElementName())) return false;
	return true;
}
/**
 * Opens and returns buffer on the source code associated with this class file.
 * Maps the source code to the children elements of this class file.
 * If no source code is associated with this class file, 
 * <code>null</code> is returned.
 * 
 * @see Openable
 */
protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaModelException {
	SourceMapper mapper = getSourceMapper();
	if (mapper != null) {
		return mapSource(mapper);
	} else if (!this.checkAutomaticSourceMapping) {
		/*
		 * We try to see if we can automatically attach a source
		 * source files located inside the same folder than its .class file
		 * See bug 36510.
		 */
		PackageFragmentRoot root = getPackageFragmentRoot();
		if (root.isArchive()) {
			// root is a jar file or a zip file
			String elementName = getElementName();
			StringBuffer sourceFileName = new StringBuffer(elementName.substring(0, elementName.lastIndexOf('.')));
			sourceFileName.append(SuffixConstants.SUFFIX_java);
			JarPackageFragmentRoot jarPackageFragmentRoot = (JarPackageFragmentRoot) root;
			ZipFile jar = null;
			try {
				jar = jarPackageFragmentRoot.getJar();
				IPackageFragment packageFragment = (IPackageFragment) getParent();
				ZipEntry zipEntry = null;
				if (packageFragment.isDefaultPackage()) {
					zipEntry = jar.getEntry(sourceFileName.toString());
				} else {
					zipEntry = jar.getEntry(getParent().getElementName() + '/' + sourceFileName.toString());
				}
				if (zipEntry != null) {
					// found a source file
					this.checkAutomaticSourceMapping = true;
					root.attachSource(root.getPath(), null, null);
					SourceMapper sourceMapper = getSourceMapper();
					if (sourceMapper != null) {
						return mapSource(sourceMapper);
					}
				}
			} catch (CoreException e) {
				if (e instanceof JavaModelException) throw (JavaModelException)e;
				throw new JavaModelException(e);
			} finally {
				JavaModelManager.getJavaModelManager().closeZipFile(jar);
			}
		} else {
			// Attempts to find the corresponding java file
			String qualifiedName = getType().getFullyQualifiedName();
			NameLookup lookup = ((JavaProject) getJavaProject()).getNameLookup();
			ICompilationUnit cu = lookup.findCompilationUnit(qualifiedName);
			if (cu != null) {
				return cu.getBuffer();
			} else	{
				// root is a class folder
				IPath sourceFilePath = getPath().removeFileExtension().addFileExtension(EXTENSION_java);
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				if (workspace == null) {
					this.checkAutomaticSourceMapping = true; // we don't want to check again
					return null; // workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=34069
				}
				if (JavaModel.getTarget(
						workspace.getRoot(),
						sourceFilePath.makeRelative(), // ensure path is relative (see http://dev.eclipse.org/bugs/show_bug.cgi?id=22517)
						true) != null) {
							
					// found a source file
					 // we don't need to check again. The source will be attached.
					this.checkAutomaticSourceMapping = true;
					root.attachSource(root.getPath(), null, null);
					SourceMapper sourceMapper = getSourceMapper();
					if (sourceMapper != null) {
						return mapSource(sourceMapper);
					}
				}
			}
		}
	}
	return null;
}
private IBuffer mapSource(SourceMapper mapper) {
	char[] contents = mapper.findSource(getType());
	if (contents != null) {
		// create buffer
		IBuffer buffer = getBufferManager().createBuffer(this);
		if (buffer == null) return null;
		BufferManager bufManager = getBufferManager();
		bufManager.addBuffer(buffer);
		
		// set the buffer source
		if (buffer.getCharacters() == null){
			buffer.setContents(contents);
		}
		
		// listen to buffer changes
		buffer.addBufferChangedListener(this);	
				
		// do the source mapping
		mapper.mapSource(getType(), contents);
		
		return buffer;
	}
	return null;
}
/* package */ static char[] simpleName(char[] className) {
	if (className == null)
		return null;
	className = unqualifiedName(className);
	int count = 0;
	int lastPosition = className.length - 1;
	for (int i = lastPosition; i > -1; i--) {
		if (className[i] == '$' && (i != lastPosition)) {
			char[] name = new char[count];
			System.arraycopy(className, i + 1, name, 0, count);
			if (Character.isDigit(name[0])) {
				break;
			}
			return name;
		}
		count++;
	}
	return className;
}
/**
 * Returns the Java Model representation of the given name
 * which is provided in diet class file format, or <code>null</code>
 * if the given name is <code>null</code>.
 *
 * <p><code>ClassFileReader</code> format is similar to "java/lang/Object",
 * and corresponding Java Model format is "java.lang.Object".
 */

public static char[] translatedName(char[] name) {
	if (name == null)
		return null;
	int nameLength = name.length;
	char[] newName= new char[nameLength];
	for (int i= 0; i < nameLength; i++) {
		if (name[i] == '/') {
			newName[i]= '.';
		} else {
			newName[i]= name[i];
		}
	}
	return newName;
}
/**
 * Returns the Java Model representation of the given names
 * which are provided in diet class file format, or <code>null</code>
 * if the given names are <code>null</code>.
 *
 * <p><code>ClassFileReader</code> format is similar to "java/lang/Object",
 * and corresponding Java Model format is "java.lang.Object".
 */

/* package */ static char[][] translatedNames(char[][] names) {
	if (names == null)
		return null;
	int length = names.length;
	char[][] newNames = new char[length][];
	for(int i = 0; i < length; i++) {
		newNames[i] = translatedName(names[i]);
	}
	return newNames;
}
/**
 * Returns the Java Model format of the unqualified class name for the
 * given className which is provided in diet class file format,
 * or <code>null</code> if the given className is <code>null</code>.
 * (This removes the package name, but not enclosing type names).
 *
 * <p><code>ClassFileReader</code> format is similar to "java/lang/Object",
 * and corresponding Java Model simple name format is "Object".
 */

/* package */ static char[] unqualifiedName(char[] className) {
	if (className == null)
		return null;
	int count = 0;
	for (int i = className.length - 1; i > -1; i--) {
		if (className[i] == '/') {
			char[] name = new char[count];
			System.arraycopy(className, i + 1, name, 0, count);
			return name;
		}
		count++;
	}
	return className;
}

/**
 * @see ICodeAssist#codeComplete(int, ICodeCompletionRequestor)
 * @deprecated - should use codeComplete(int, ICompletionRequestor) instead
 */
public void codeComplete(int offset, final org.eclipse.jdt.core.ICodeCompletionRequestor requestor) throws JavaModelException {
	
	if (requestor == null){
		codeComplete(offset, (ICompletionRequestor)null);
		return;
	}
	codeComplete(
		offset,
		new ICompletionRequestor(){
			public void acceptAnonymousType(char[] superTypePackageName,char[] superTypeName, char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
				// ignore
			}
			public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
				requestor.acceptClass(packageName, className, completionName, modifiers, completionStart, completionEnd);
			}
			public void acceptError(IProblem error) {
				if (true) return; // was disabled in 1.0

				try {
					IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IJavaModelMarker.TRANSIENT_PROBLEM);
					marker.setAttribute(IJavaModelMarker.ID, error.getID());
					marker.setAttribute(IMarker.CHAR_START, error.getSourceStart());
					marker.setAttribute(IMarker.CHAR_END, error.getSourceEnd() + 1);
					marker.setAttribute(IMarker.LINE_NUMBER, error.getSourceLineNumber());
					marker.setAttribute(IMarker.MESSAGE, error.getMessage());
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					requestor.acceptError(marker);
				} catch(CoreException e){
					// marker could not be created: ignore
				}
			}
			public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] fieldName, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
				requestor.acceptField(declaringTypePackageName, declaringTypeName, fieldName, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd);
			}
			public void acceptInterface(char[] packageName,char[] interfaceName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
				requestor.acceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd);
			}
			public void acceptKeyword(char[] keywordName,int completionStart,int completionEnd, int relevance){
				requestor.acceptKeyword(keywordName, completionStart, completionEnd);
			}
			public void acceptLabel(char[] labelName,int completionStart,int completionEnd, int relevance){
				requestor.acceptLabel(labelName, completionStart, completionEnd);
			}
			public void acceptLocalVariable(char[] localVarName,char[] typePackageName,char[] typeName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			public void acceptMethod(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// skip parameter names
				requestor.acceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd);
			}
			public void acceptMethodDeclaration(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
				// ignore
			}
			public void acceptModifier(char[] modifierName,int completionStart,int completionEnd, int relevance){
				requestor.acceptModifier(modifierName, completionStart, completionEnd);
			}
			public void acceptPackage(char[] packageName,char[] completionName,int completionStart,int completionEnd, int relevance){
				requestor.acceptPackage(packageName, completionName, completionStart, completionEnd);
			}
			public void acceptType(char[] packageName,char[] typeName,char[] completionName,int completionStart,int completionEnd, int relevance){
				requestor.acceptType(packageName, typeName, completionName, completionStart, completionEnd);
			}
			public void acceptVariableName(char[] typePackageName,char[] typeName,char[] varName,char[] completionName,int completionStart,int completionEnd, int relevance){
				// ignore
			}
		});
}
}
