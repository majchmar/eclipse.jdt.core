/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.parser;

/**
 * IMPORTANT NOTE: These constants are dedicated to the internal Scanner implementation.
 * It is mirrored in org.eclipse.jdt.core.compiler public package where it is API.
 * The mirror implementation is using the backward compatible ITerminalSymbols constant
 * definitions (stable with 2.0), whereas the internal implementation uses TerminalTokens
 * which constant values reflect the latest parser generation state.
 */
/**
 * Maps each terminal symbol in the java-grammar into a unique integer.
 * This integer is used to represent the terminal when computing a parsing action.
 *
 * Disclaimer : These constant values are generated automatically using a Java
 * grammar, therefore their actual values are subject to change if new keywords
 * were added to the language (for instance, 'assert' is a keyword in 1.4).
 */
public interface TerminalTokens {

	// special tokens not part of grammar - not autogenerated
	int TokenNameWHITESPACE = 1000,
		TokenNameCOMMENT_LINE = 1001,
		TokenNameCOMMENT_BLOCK = 1002,
		TokenNameCOMMENT_JAVADOC = 1003;

	int TokenNameIdentifier = 23,
		TokenNameabstract = 33,
		TokenNameassert = 75,
		TokenNameboolean = 44,
		TokenNamebreak = 76,
		TokenNamebyte = 45,
		TokenNamecase = 102,
		TokenNamecatch = 103,
		TokenNamechar = 46,
		TokenNameclass = 71,
		TokenNamecontinue = 77,
		TokenNameconst = 108,
		TokenNamedefault = 87,
		TokenNamedo = 78,
		TokenNamedouble = 47,
		TokenNameelse = 104,
		TokenNameenum = 100,
		TokenNameextends = 88,
		TokenNamefalse = 56,
		TokenNamefinal = 34,
		TokenNamefinally = 105,
		TokenNamefloat = 48,
		TokenNamefor = 79,
		TokenNamegoto = 109,
		TokenNameif = 80,
		TokenNameimplements = 107,
		TokenNameimport = 101,
		TokenNameinstanceof = 13,
		TokenNameint = 49,
		TokenNameinterface = 85,
		TokenNamelong = 50,
		TokenNamenative = 35,
		TokenNamenew = 55,
		TokenNamenull = 57,
		TokenNamepackage = 86,
		TokenNameprivate = 36,
		TokenNameprotected = 37,
		TokenNamepublic = 38,
		TokenNamereturn = 81,
		TokenNameshort = 51,
		TokenNamestatic = 32,
		TokenNamestrictfp = 39,
		TokenNamesuper = 53,
		TokenNameswitch = 82,
		TokenNamesynchronized = 40,
		TokenNamethis = 54,
		TokenNamethrow = 83,
		TokenNamethrows = 106,
		TokenNametransient = 41,
		TokenNametrue = 58,
		TokenNametry = 84,
		TokenNamevoid = 52,
		TokenNamevolatile = 42,
		TokenNamewhile = 73,
		TokenNameIntegerLiteral = 59,
		TokenNameLongLiteral = 60,
		TokenNameFloatingPointLiteral = 61,
		TokenNameDoubleLiteral = 62,
		TokenNameCharacterLiteral = 63,
		TokenNameStringLiteral = 64,
		TokenNamePLUS_PLUS = 9,
		TokenNameMINUS_MINUS = 10,
		TokenNameEQUAL_EQUAL = 18,
		TokenNameLESS_EQUAL = 15,
		TokenNameGREATER_EQUAL = 16,
		TokenNameNOT_EQUAL = 19,
		TokenNameLEFT_SHIFT = 17,
		TokenNameRIGHT_SHIFT = 11,
		TokenNameUNSIGNED_RIGHT_SHIFT = 12,
		TokenNamePLUS_EQUAL = 89,
		TokenNameMINUS_EQUAL = 90,
		TokenNameMULTIPLY_EQUAL = 91,
		TokenNameDIVIDE_EQUAL = 92,
		TokenNameAND_EQUAL = 93,
		TokenNameOR_EQUAL = 94,
		TokenNameXOR_EQUAL = 95,
		TokenNameREMAINDER_EQUAL = 96,
		TokenNameLEFT_SHIFT_EQUAL = 97,
		TokenNameRIGHT_SHIFT_EQUAL = 98,
		TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL = 99,
		TokenNameOR_OR = 27,
		TokenNameAND_AND = 26,
		TokenNamePLUS = 1,
		TokenNameMINUS = 2,
		TokenNameNOT = 67,
		TokenNameREMAINDER = 5,
		TokenNameXOR = 22,
		TokenNameAND = 20,
		TokenNameMULTIPLY = 4,
		TokenNameOR = 24,
		TokenNameTWIDDLE = 68,
		TokenNameDIVIDE = 6,
		TokenNameGREATER = 14,
		TokenNameLESS = 7,
		TokenNameLPAREN = 29,
		TokenNameRPAREN = 30,
		TokenNameLBRACE = 69,
		TokenNameRBRACE = 43,
		TokenNameLBRACKET = 8,
		TokenNameRBRACKET = 70,
		TokenNameSEMICOLON = 28,
		TokenNameQUESTION = 25,
		TokenNameCOLON = 65,
		TokenNameCOMMA = 31,
		TokenNameDOT = 3,
		TokenNameEQUAL = 74,
		TokenNameAT = 21,
		TokenNameELLIPSIS = 72,
		TokenNameEOF = 66,
		TokenNameERROR = 110;
}
