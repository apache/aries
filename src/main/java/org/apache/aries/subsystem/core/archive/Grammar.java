/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.archive;


public interface Grammar {
//	  section:                       *header +newline 
//	  nonempty-section:      +header +newline 
//	  newline:                      CR LF | LF | CR (not followed by LF) 
//	  header:                       name : value 
//	  name:                         alphanum *headerchar 
//	  value:                          SPACE *otherchar newline *continuation 
//	  continuation:              SPACE *otherchar newline 
//	  alphanum:                  {A-Z} | {a-z} | {0-9} 
//	  headerchar:                alphanum | - | _ 
//	  otherchar:                  any UTF-8 character except NUL, CR and LF
	
//	public static final String ALPHA = "[A-Za-z]";
//	public static final String DIGIT = "[0-9]";
//	public static final String ALPHANUM = ALPHA + '|' + DIGIT;
//	public static final String NEWLINE = "\r\n|\n|\r";
//	public static final String OTHERCHAR = "[^\u0000\r\n]";
//	public static final String SPACE = " ";
//	public static final String CONTINUATION = SPACE + OTHERCHAR + "*(?:" + NEWLINE + ')';
//	public static final String HEADERCHAR = ALPHANUM + "|-|_";
//	public static final String NAME = "(?:" + ALPHANUM + ")(?:" + HEADERCHAR + ")*";
//	public static final String VALUE = SPACE + OTHERCHAR + "*(?:" + NEWLINE + ")(?:" + CONTINUATION + ")*";
//	public static final String HEADER = NAME + ':' + VALUE;
//	public static final String SECTION = "(?:" + HEADER + ")*(?:" + NEWLINE + ")+";
//	public static final String NONEMPTY_SECTION = "(?:" + HEADER + ")+(?:" + NEWLINE + ")+";
	
//	  manifest-file:                    main-section newline *individual-section 
//	  main-section:                    version-info newline *main-attribute 
//	  version-info:                      Manifest-Version : version-number 
//	  version-number :               digit+{.digit+}* 
//	  main-attribute:                 (any legitimate main attribute) newline 
//	  individual-section:             Name : value newline *perentry-attribute 
//	  perentry-attribute:            (any legitimate perentry attribute) newline  
//	   digit:                                {0-9}
	
//	public static final String VERSION_NUMBER = DIGIT + "+(?:\\." + DIGIT + "+)*";
//	public static final String VERSION_INFO = "Manifest-Version: " + VERSION_NUMBER;
//	public static final String MAIN_ATTRIBUTE = HEADER + NEWLINE;
//	public static final String MAIN_SECTION = VERSION_INFO + NEWLINE + "(?:" + MAIN_ATTRIBUTE + ")*";
//	public static final String PERENTRY_ATTRIBUTE = HEADER + NEWLINE;
//	public static final String INDIVIDUAL_SECTION = "Name: " + VALUE + NEWLINE + "(?:" + PERENTRY_ATTRIBUTE + ")*";
//	public static final String MANIFEST_FILE = MAIN_SECTION + NEWLINE + "(?:" + INDIVIDUAL_SECTION + ")*";
	
//	digit ::= [0..9]
//	alpha ::= [a..zA..Z]
//	alphanum ::= alpha | digit
//	extended ::= ( alphanum | _ | - | . )+
//	quoted-string ::= " ( ~["\#x0D#x0A#x00] | \"|\\)* "
//	argument ::= extended | quoted-string
//	parameter ::= directive | attribute
//	directive ::= extended := argument
//	attribute ::= extended = argument
//	path ::= path-unquoted | (" path-unquoted ")
//	path-unquoted ::= path-sep | path-sep? path-element (path-sep path-element)*
//	path-element ::= ~[/"\#x0D#x0A#x00]+
//	path-sep ::= /
//	header ::= clause ( , clause ) *
//	clause ::= path ( ; path ) * ( ; parameter ) *
	
	public static final String DIGIT = "[0-9]";
	public static final String ALPHA = "[A-Za-z]";
	public static final String ALPHANUM = DIGIT + '|' + ALPHA;
	public static final String TOKEN = "(?:" + ALPHANUM + "|_|-)+";
	public static final String EXTENDED = "(?:" + ALPHANUM + "|_|-|\\.)+";
	public static final String QUOTED_STRING = "\"(?:[^\\\\\"\r\n\u0000]|\\\\\"|\\\\\\\\)*\"";
	public static final String ARGUMENT = EXTENDED + '|' + QUOTED_STRING;
	public static final String DIRECTIVE = EXTENDED + ":=(?:" + ARGUMENT + ')';
	public static final String ATTRIBUTE = EXTENDED + "=(?:" + ARGUMENT + ')';
	public static final String PARAMETER = "(?:" + DIRECTIVE + ")|(?:" + ATTRIBUTE + ')';
	public static final String PATH_ELEMENT = "[^/\"\r\n\u0000]+";
	public static final String PATH_ELEMENT_NT = "[^/\"\r\n\u0000\\:=;, ]+";
	public static final String PATH_SEP = "/";
	public static final String PATH_UNQUOTED = PATH_SEP + '|' + PATH_SEP + '?' + PATH_ELEMENT + "(?:" + PATH_SEP + PATH_ELEMENT + ")*";
	public static final String PATH_UNQUOTED_NT = PATH_SEP + '|' + PATH_SEP + '?' + PATH_ELEMENT_NT + "(?:" + PATH_SEP + PATH_ELEMENT_NT + ")*";
	public static final String PATH = "(?:" + PATH_UNQUOTED_NT + ")|\"(?:" + PATH_UNQUOTED + ")\"";
	public static final String CLAUSE = "(?:" + PATH + ")(?:;" + PATH + ")*(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String HEADERCHAR = ALPHANUM + "|_|-";
	public static final String NAME = ALPHANUM + "(?:" + HEADERCHAR + ")*";
	public static final String HEADER = NAME + ": " + CLAUSE + "(?:," + CLAUSE + ")*";
	
	/*
	 * jletter ::= a character for which the method Character.isJavaIdentifierStart(int) returns true
	 * jletterordigit::= a character for which the method Character.isJavaIdentifierPart(int) returns true
	 * identifier ::= jletter jletterordigit *
	 * unique-name ::= identifier ( ’.’ identifier )*
	 * package-name ::= unique-name
	 * Import-Package ::= import ( ',' import )*
	 * import ::= package-names ( ';' parameter )*
	 * package-names ::= package-name ( ';' package-name )* // See 1.3.2
	 */
	
	public static final String JLETTER = "\\p{javaJavaIdentifierStart}";
	public static final String JLETTERORDIGIT = "\\p{javaJavaIdentifierPart}";
	public static final String IDENTIFIER = JLETTER + "(?:" + JLETTERORDIGIT + ")*";
	public static final String UNIQUENAME = IDENTIFIER + "(?:\\." + IDENTIFIER + ")*";
	public static final String SYMBOLICNAME = TOKEN + "(?:\\." + TOKEN + ")*";
	public static final String PACKAGENAME = UNIQUENAME;
	public static final String PACKAGENAMES = PACKAGENAME + "\\s*(?:\\;\\s*" + PACKAGENAME + ")*";
	public static final String IMPORT = PACKAGENAMES + "(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String IMPORTPACKAGE = IMPORT + "(?:\\,\\s*" + IMPORT + ")*";
	
	public static final String NAMESPACE = SYMBOLICNAME;
	public static final String REQUIREMENT = NAMESPACE + "(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String REQUIRE_CAPABILITY = REQUIREMENT + "(?:,\\s*(?:" + REQUIREMENT + "))*";
	
	public static final String BUNDLE_DESCRIPTION = SYMBOLICNAME + "(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String REQUIRE_BUNDLE = BUNDLE_DESCRIPTION + "(?:,\\s*(?:" + BUNDLE_DESCRIPTION + "))*";
	
	public static final String EXPORT = PACKAGENAMES + "(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String EXPORT_PACKAGE = EXPORT + "(?:,\\s*(?:" + EXPORT + "))*";
	
	public static final String SCALAR = "String|Version|Long|Double";
	public static final String LIST = "List<(?:" + SCALAR + ")>";
	public static final String TYPE = "(?:" + SCALAR + ")|" + LIST;
	public static final String TYPED_ATTR = EXTENDED + "(?:\\:" + TYPE + ")?=(?:" + ARGUMENT + ')';
	public static final String CAPABILITY = NAMESPACE + "(?:;\\s*(?:(?:" + DIRECTIVE + ")|(?:" + TYPED_ATTR + ")))*";
	public static final String PROVIDE_CAPABILITY = CAPABILITY + "(?:,\\s*(?:" + CAPABILITY + "))*";
	
	public static final String OBJECTCLASS = PACKAGENAME;
	public static final String SERVICE = OBJECTCLASS + "(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String SUBSYSTEM_EXPORTSERVICE = SERVICE + "(?:,\\s*(?:" + SERVICE + "))*";
	public static final String SUBSYSTEM_IMPORTSERVICE = SERVICE + "(?:,\\s*(?:" + SERVICE + "))*";
	
	public static final String RESOURCE = SYMBOLICNAME + "(?:;\\s*(?:" + PARAMETER + "))*";
	public static final String PREFERRED_PROVIDER = RESOURCE + "(?:,\\s*(?:" + RESOURCE + "))*";
	
	/*
	 * number ::= digit+
	 * version ::= major( '.' minor ( '.' micro ( '.' qualifier )? )? )?
	 * major ::= number // See 1.3.2
	 * minor ::= number
	 * micro ::= number
	 * qualifier ::= ( alphanum | ’_’ | '-' )+
	 * version-range ::= interval | atleast
	 * interval ::= ( '[' | '(' ) floor ',' ceiling ( ']' | ')' )
	 * atleast ::= version
	 * floor ::= version
	 * ceiling ::= version
	 */
	
	public static final String NUMBER = DIGIT + '+';
	public static final String MAJOR = NUMBER;
	public static final String MINOR = NUMBER;
	public static final String MICRO = NUMBER;
	public static final String QUALIFIER = "(?:" + ALPHANUM + "|_|-)+";
	public static final String VERSION = MAJOR + "(?:\\." + MINOR + "(?:\\." + MICRO + "(?:\\." + QUALIFIER + ")?)?)?";
	public static final String ATLEAST = VERSION;
	public static final String FLOOR = VERSION;
	public static final String CEILING = VERSION;
	public static final String INTERVAL = "[\\[\\(]" + FLOOR + ',' + CEILING + "[\\[\\)]";
	public static final String VERSIONRANGE = INTERVAL + '|' + ATLEAST;
}
